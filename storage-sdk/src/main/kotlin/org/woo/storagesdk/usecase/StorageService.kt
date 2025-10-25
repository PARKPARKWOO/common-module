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

        val builder =
            GetPresignedDownloadUrlRequest
                .newBuilder()
                .setBucket(bucket)
                .setObjectKey(objectKey)
                .setExpirySeconds(expirySeconds)

        responseContentType?.takeIf { it.isNotBlank() }?.let {
            builder.setResponseContentType(responseContentType)
        }

        responseContentDisposition?.takeIf { it.isNotBlank() }?.let {
            builder.setResponseContentDisposition(it)
        }

        return uploadStubToMinio.getPresignedDownloadUrl(builder.build()).url
    }

    override suspend fun uploadStreamToMinio(
        spec: MinioUploadSpec,
        vararg interceptors: ClientInterceptor,
    ): MinioUploadResponse {
        require(uploadStubToMinio != null) { "minio stub is null" }

        val stub =
            uploadStubToMinio!!
                .withInterceptors(*interceptors)
        // .withCallCredentials(Bearer(jwt)) // 필요 시
        // .withDeadlineAfter(60, TimeUnit.SECONDS) // 옵션

        val headerBuilder =
            FileUploadSpec
                .newBuilder()
                .setBucket(spec.header.applicationId)
                .setUploadedBy(spec.header.uploadedBy)
                .setObjectKey(spec.header.objectKey)
                .setContentType(spec.header.contentType ?: "application/octet-stream")

        spec.header.contentDisposition?.takeIf { it.isNotBlank() }?.let {
            headerBuilder.setContentDisposition(it)
        }

        spec.header.metadata?.forEach { (k, v) -> headerBuilder.putMetadata(k, v) }

        val chunkSize = 64 * 1024 // 64KB (서버가 MinIO로 단일 스트림 put이면 충분)
        val maxBytes = spec.header.contentLength.takeIf { it > 0 } // 선택: 상한

        val requestFlow =
            flow {
                emit(UploadFileRequest.newBuilder().setHeader(headerBuilder.build()).build())
                var sent = 0L
                val buf = ByteArray(chunkSize)

                spec.data.use { input ->
                    while (true) {
                        if (maxBytes != null && sent >= maxBytes) break
                        val read = input.read(buf)
                        if (read == -1) break
                        if (maxBytes != null && sent + read > maxBytes) {
                            // 상한 초과 방지: 초과분 잘라내기
                            val allow = (maxBytes - sent).toInt()
                            if (allow <= 0) break
                            emit(
                                UploadFileRequest
                                    .newBuilder()
                                    .setChunk(ByteString.copyFrom(buf, 0, allow))
                                    .build(),
                            )
                            sent += allow
                            break
                        } else {
                            emit(
                                UploadFileRequest
                                    .newBuilder()
                                    .setChunk(ByteString.copyFrom(buf, 0, read))
                                    .build(),
                            )
                            sent += read
                        }
                    }
                }
                // (옵션) sent < required 최소 크기면 INVALID_ARGUMENT로 실패 유도 가능
            }.flowOn(uploadDispatcher)

        val response = uploadStubToMinio.uploadFile(requestFlow)
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
