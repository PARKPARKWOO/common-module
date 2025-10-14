package org.woo.storagesdk.usecase

import com.example.grpc.fileupload.FileData
import com.example.grpc.fileupload.FileUploadChunk
import com.example.grpc.fileupload.FileUploadServiceGrpcKt
import com.example.grpc.fileupload.FileUploadSpec
import com.example.grpc.fileupload.GetPresignedDownloadUrlRequest
import com.example.grpc.fileupload.GetPresignedUploadUrlRequest
import com.example.grpc.fileupload.StorageServiceGrpcKt
import com.example.grpc.fileupload.UploadFileRequest
import com.google.protobuf.ByteString
import io.grpc.ClientInterceptor
import io.grpc.Status
import io.grpc.StatusException
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
import org.woo.grpc.circuitbreaker.GrpcCircuitBreaker
import org.woo.grpc.circuitbreaker.ServiceStateRegistry
import org.woo.storagesdk.dto.MinioUploadResponse
import org.woo.storagesdk.dto.MinioUploadSpec
import org.woo.storagesdk.exception.FileUploadException
import org.woo.storagesdk.exception.InvalidArgumentException
import org.woo.storagesdk.exception.MaxChunkSizeExceededException
import org.woo.storagesdk.exception.PermissionDeniedException
import org.woo.storagesdk.exception.ResourceExhaustedException
import org.woo.storagesdk.exception.ServiceUnavailableException
import org.woo.storagesdk.exception.TimeoutException
import org.woo.storagesdk.interceptor.UploadInterceptor
import java.io.InputStream
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

class StorageService(
    private val uploadStubToCassandra: FileUploadServiceGrpcKt.FileUploadServiceCoroutineStub? = null,
    private val uploadStubToMinio: StorageServiceGrpcKt.StorageServiceCoroutineStub? = null,
    private val cpuCore: Int = DEFAULT_CORE_SIZE,
    private val uploadInterceptors: List<UploadInterceptor>,
    private val circuitBreaker: GrpcCircuitBreaker,
) : StorageClient {
    companion object {
        private const val MAX_CHUNK_SIZE = 4000_000L
        private const val DEFAULT_BUFFER_SIZE = 8192
        private const val DEFAULT_CORE_SIZE = 4
        private const val UPLOAD_STREAM_METHOD_NAME = "uploadFileStream"
    }

    private val ioDispatcher = Dispatchers.IO

    private lateinit var uploadDispatcher: ExecutorCoroutineDispatcher

    @PostConstruct
    fun init() {
        uploadDispatcher = Executors.newFixedThreadPool(cpuCore).asCoroutineDispatcher()
        ServiceStateRegistry.initGrpcService(FileUploadServiceGrpcKt.serviceDescriptor)
    }

    @PreDestroy
    fun destroy() {
        uploadDispatcher.close()
    }

    @Throws(FileUploadException::class)
    override suspend fun uploadStream(
        fileOriginName: String,
        uploadedBy: String,
        contentLength: Long,
        chunkSize: Int,
        applicationId: String,
        data: InputStream,
        accessLevel: Int,
        vararg interceptors: ClientInterceptor,
    ): Long {
        if (uploadStubToCassandra == null) {
            throw IllegalArgumentException("cassandra stub is null")
        }
        circuitBreaker.checkCircuitBreaker(UPLOAD_STREAM_METHOD_NAME)
        // 청크 크기 검증
        val effectiveChunkSize =
            when {
                chunkSize <= 0 -> DEFAULT_BUFFER_SIZE
                chunkSize > MAX_CHUNK_SIZE -> throw MaxChunkSizeExceededException("최대 청크 크기는 ${MAX_CHUNK_SIZE}입니다.")
                else -> chunkSize
            }
        val finalStreams =
            uploadInterceptors.fold(data) { acc, uploadInterceptor ->
                uploadInterceptor.call(acc, fileOriginName)
            }
        // 총 페이지 수 계산
        val pageSize = ceil(contentLength.toDouble() / effectiveChunkSize).toInt()

        return withContext(ioDispatcher) {
            try {
                val baseChunkBuilder =
                    FileUploadChunk
                        .newBuilder()
                        .setFileName(fileOriginName)
                        .setUploadedBy(uploadedBy)
                        .setContentLength(contentLength)
                        .setChunkSize(effectiveChunkSize)
                        .setApplicationId(applicationId)
                        .setAccessLevel(accessLevel)
                        .setPageSize(pageSize)

                val stubWithInterceptors =
                    if (interceptors.isNotEmpty()) {
                        uploadStubToCassandra.withInterceptors(*interceptors)
                    } else {
                        uploadStubToCassandra
                    }
                val chunkFlow = createChunkFlow(finalStreams, effectiveChunkSize, baseChunkBuilder)

                // 파일 청크 스트림 전송 및 응답 처리
                val response =
                    stubWithInterceptors
                        .uploadFileStream(chunkFlow)
                        .last()
                response.message
            } catch (e: StatusException) {
                handleGrpcException(e)
            } catch (e: Exception) {
                throw FileUploadException("파일 업로드 중 오류 발생: ${e.message}")
            } finally {
                try {
                    data.close()
                } catch (e: Exception) {
                    println("리소스 정리 중 오류 발생: ${e.message}")
                }
            }
        }
    }

    private fun createChunkFlow(
        data: InputStream,
        chunkSize: Int,
        baseChunkBuilder: FileUploadChunk.Builder,
    ): Flow<FileUploadChunk> =
        flow {
            val buffer = ByteArray(chunkSize)
            var bytesRead: Int
            val offset = AtomicInteger(0)
            while (data.read(buffer).also { bytesRead = it } != -1) {
                // 실제로 읽은 바이트만 포함
                val actualData =
                    if (bytesRead < chunkSize) {
                        buffer.copyOf(bytesRead)
                    } else {
                        buffer.clone() // 원본 버퍼 보존을 위해 복제
                    }
                val fileData =
                    FileData
                        .newBuilder()
                        .setOffset(offset.getAndIncrement())
                        .setData(ByteString.copyFrom(actualData))
                        .build()

                val chunk =
                    baseChunkBuilder
                        .clone()
                        .setFileData(fileData)
                        .build()

                emit(chunk)
            }
        }.flowOn(uploadDispatcher)

    private fun handleGrpcException(e: StatusException): Nothing {
        when (e.status.code) {
            Status.Code.INVALID_ARGUMENT -> throw InvalidArgumentException("잘못된 인수: ${e.message}")
            Status.Code.RESOURCE_EXHAUSTED -> throw ResourceExhaustedException("리소스 한계 초과: ${e.message}")
            Status.Code.UNAVAILABLE -> throw ServiceUnavailableException("서비스를 사용할 수 없음: ${e.message}")
            Status.Code.DEADLINE_EXCEEDED -> throw TimeoutException("요청 시간 초과: ${e.message}")
            Status.Code.PERMISSION_DENIED -> throw PermissionDeniedException("권한 없음: ${e.message}")
            else -> throw FileUploadException("파일 업로드 실패: ${e.status.code} - ${e.message}")
        }
    }

    override suspend fun getUploadPresignUrl(
        applicationId: String,
        expiry: Int,
        objectKey: String,
        fileLength: Long?,
        contentType: String,
        vararg interceptors: ClientInterceptor,
    ): String {
        require(uploadStubToMinio != null) { "minio stub is null" }

        val spec =
            FileUploadSpec
                .newBuilder()
                .setBucket(applicationId)
                .setObjectKey(objectKey) // ex) "apps/$applicationId/${UUID.randomUUID()}.png"
                .setContentType(contentType) // ex) "image/png"
                .build()

        val req =
            GetPresignedUploadUrlRequest
                .newBuilder()
                .setSpec(spec)
                .apply { if (fileLength != null && fileLength > 0) setContentLength(fileLength) }
                .setIdempotencyKey(UUID.randomUUID().toString()) // 같은 파일 재시도에 유용
                .setExpirySeconds(expiry) // ex) 600
                // .setChecksumSha256Base64(checksumBase64)      // 무결성 쓰려면 사용
                .build()

        val response = uploadStubToMinio.getPresignedUploadUrl(req)
        return response.url
    }

    override suspend fun getDownloadPresignedUrl(
        bucket: String,
        objectKey: String,
        expirySeconds: Int,
        responseContentType: String?,
        responseContentDisposition: String?,
        vararg interceptors: ClientInterceptor,
    ): String {
        require(uploadStubToMinio != null) { "minio stub is null" }

        val request =
            GetPresignedDownloadUrlRequest
                .newBuilder()
                .setBucket(bucket)
                .setObjectKey(objectKey)
                .setExpirySeconds(expirySeconds)
                .setResponseContentType(responseContentType)
                .setResponseContentDisposition(responseContentDisposition)
                .build()
        return uploadStubToMinio.getPresignedDownloadUrl(request).url
    }

    override suspend fun uploadStreamToMinio(
        spec: MinioUploadSpec,
        vararg interceptors: ClientInterceptor,
    ): MinioUploadResponse {
        require(uploadStubToMinio != null) { "minio stub is null" }
        val request =
            flow {
                val header =
                    FileUploadSpec
                        .newBuilder()
                        .setBucket(spec.header.applicationId)
                        .setObjectKey(spec.header.objectKey)
                        .setContentType(spec.header.contentType ?: "application/octet-stream")
                        .setContentDisposition(spec.header.contentDisposition ?: "")
                        .build()
                emit(UploadFileRequest.newBuilder().setHeader(header).build())

                val buf = ByteArray(spec.header.contentLength)
                while (true) {
                    val read = spec.data.read(buf)
                    if (read <= 0) break
                    emit(
                        UploadFileRequest
                            .newBuilder()
                            .setChunk(ByteString.copyFrom(buf, 0, read))
                            .build(),
                    )
                }
            }.flowOn(uploadDispatcher)

        val response = uploadStubToMinio.uploadFile(request)
        return MinioUploadResponse(
            bucket = response.bucket,
            objectKey = response.objectKey,
            size = response.size,
            etag = response.etag,
        )
    }

    private fun canonicalContentTypeFromExt(ext: String): String? =
        when (ext.lowercase()) {
            ".jpg", ".jpeg" -> "image/jpeg"
            ".png" -> "image/png"
            ".gif" -> "image/gif"
            ".webp" -> "image/webp"
            ".heic", ".heif" -> "image/heic"
            ".pdf" -> "application/pdf"
            ".zip" -> "application/zip"
            ".mp4" -> "video/mp4"
            ".webm" -> "video/webm"
            ".mp3" -> "audio/mpeg"
            ".wav", ".wave" -> "audio/wav"
            else -> null
        }

    private fun String.safeObjectKey(): String {
        // 앞 슬래시 금지, 경로 역참조 금지, 허용문자만
        require(!startsWith("/")) { "objectKey must not start with '/'" }
        require(!contains("..")) { "objectKey must not contain '..'" }
        require(Regex("^[A-Za-z0-9!._\\-~/]+$").matches(this)) { "objectKey contains illegal chars" }
        // 중복 슬래시 정리
        return replace(Regex("/{2,}"), "/")
    }
}
