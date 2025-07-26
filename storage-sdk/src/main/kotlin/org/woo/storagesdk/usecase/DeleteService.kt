package org.woo.storagesdk.usecase

import com.example.grpc.filedelete.DeleteFileRequest
import com.example.grpc.filedelete.FileDeleteServiceGrpcKt
import io.grpc.ClientInterceptor
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.woo.grpc.circuitbreaker.GrpcCircuitBreaker
import org.woo.grpc.circuitbreaker.ServiceStateRegistry

class DeleteService(
    private val stub: FileDeleteServiceGrpcKt.FileDeleteServiceCoroutineStub,
    private val circuitBreaker: GrpcCircuitBreaker,
    private val dispatcher: ExecutorCoroutineDispatcher,
) : DeleteClient {
    companion object {
        private const val DELETE_SERVICE_NAME = "delete"
    }

    init {
        ServiceStateRegistry.initGrpcService(FileDeleteServiceGrpcKt.serviceDescriptor)
    }

    override suspend fun delete(
        id: Long,
        vararg interceptors: ClientInterceptor,
    ) {
        val request =
            DeleteFileRequest
                .newBuilder()
                .setId(id)
                .build()
        circuitBreaker.checkCircuitBreaker(DELETE_SERVICE_NAME)

        withContext(dispatcher) {
            launch {
                val stubWithInterceptors =
                    if (interceptors.isNotEmpty()) {
                        stub.withInterceptors(*interceptors)
                    } else {
                        stub
                    }
                stubWithInterceptors
                    .delete(request)
            }
        }
    }
}
