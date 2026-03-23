package org.woo.storagesdk.usecase

import io.grpc.ClientInterceptor

interface MinioAdminClient {
    suspend fun deleteObject(
        bucket: String,
        objectKey: String,
        vararg interceptors: ClientInterceptor,
    )
}
