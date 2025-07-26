package org.woo.storagesdk.usecase

import io.grpc.ClientInterceptor

interface DeleteClient {
    suspend fun delete(
        id: Long,
        vararg interceptors: ClientInterceptor,
    )
}
