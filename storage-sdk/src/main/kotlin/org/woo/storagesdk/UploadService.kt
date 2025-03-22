package org.woo.storagesdk

import com.example.grpc.fileupload.FileData
import com.example.grpc.fileupload.FileUploadChunk
import com.example.grpc.fileupload.FileUploadServiceGrpcKt
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.StatusException
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.Throws
import kotlin.math.ceil

class UploadService(
    private val stub: FileUploadServiceGrpcKt.FileUploadServiceCoroutineStub,
    private val cpuCore: Int = DEFAULT_CORE_SIZE,
) : UploadClient {
    companion object {
        private const val MAX_CHUNK_SIZE = 4000_000L
        private const val DEFAULT_BUFFER_SIZE = 8192
        private const val DEFAULT_CORE_SIZE = 4
    }

    private val ioDispatcher = Dispatchers.IO

    private lateinit var uploadDispatcher: ExecutorCoroutineDispatcher

    @PostConstruct
    fun init() {
        uploadDispatcher = Executors.newFixedThreadPool(cpuCore).asCoroutineDispatcher()
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
        applicationId: Long,
        data: InputStream,
    ): Long {
        // 청크 크기 검증
        val effectiveChunkSize =
            when {
                chunkSize <= 0 -> DEFAULT_BUFFER_SIZE
                chunkSize > MAX_CHUNK_SIZE -> throw MaxChunkSizeExceededException("최대 청크 크기는 ${MAX_CHUNK_SIZE}입니다.")
                else -> chunkSize
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
                        .setPageSize(pageSize)

                val chunkFlow = createChunkFlow(data, effectiveChunkSize, baseChunkBuilder)

                // 파일 청크 스트림 전송 및 응답 처리
                val response = stub.uploadFileStream(chunkFlow).last()
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

            withContext(uploadDispatcher) {
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
                            .setOffset(offset.getAndIncrement()) // 증가 전 값 반환
                            .setData(ByteString.copyFrom(actualData))
                            .build()

                    val chunk =
                        baseChunkBuilder
                            .clone()
                            .setFileData(fileData)
                            .build()

                    emit(chunk)
                }
            }
        }

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
}
