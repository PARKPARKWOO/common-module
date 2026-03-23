package org.woo.storagesdk.usecase

import com.example.grpc.minioadmin.DeleteObjectRequest
import com.example.grpc.minioadmin.MinioAdminServiceGrpcKt
import io.grpc.ClientInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.woo.grpc.circuitbreaker.GrpcCircuitBreaker
import org.woo.grpc.circuitbreaker.ServiceStateRegistry

class MinioAdminService(
    private val stub: MinioAdminServiceGrpcKt.MinioAdminServiceCoroutineStub,
    private val circuitBreaker: GrpcCircuitBreaker,
    private val dispatcher: CoroutineDispatcher,
) : MinioAdminClient {
    companion object {
        private const val DELETE_OBJECT_METHOD_NAME = "deleteObject"
    }

    init {
        ServiceStateRegistry.initGrpcService(MinioAdminServiceGrpcKt.serviceDescriptor)
    }

    override suspend fun deleteObject(
        bucket: String,
        objectKey: String,
        vararg interceptors: ClientInterceptor,
    ) {
        val request =
            DeleteObjectRequest
                .newBuilder()
                .setBucket(bucket)
                .setObjectKey(objectKey)
                .build()
        circuitBreaker.checkCircuitBreaker(DELETE_OBJECT_METHOD_NAME)

        withContext(dispatcher) {
            val stubWithInterceptors =
                if (interceptors.isNotEmpty()) {
                    stub.withInterceptors(*interceptors)
                } else {
                    stub
                }
            stubWithInterceptors.deleteObject(request)
        }
    }
}
