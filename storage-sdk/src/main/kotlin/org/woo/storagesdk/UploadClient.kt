package org.woo.storagesdk

import io.grpc.ClientInterceptor
import java.io.InputStream

interface UploadClient {
    companion object {
        const val PUBLIC_ACCESS_LEVEL = 0
    }

    // id 값 리턴
    suspend fun uploadStream(
        fileOriginName: String,
        uploadedBy: String,
        contentLength: Long,
        chunkSize: Int,
        applicationId: String,
        data: InputStream,
        accessLevel: Int = PUBLIC_ACCESS_LEVEL,
        vararg interceptors: ClientInterceptor = emptyArray(),
    ): Long
}
