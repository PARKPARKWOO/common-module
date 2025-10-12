package org.woo.storagesdk.usecase

import io.grpc.ClientInterceptor
import java.io.InputStream

interface StorageClient {
    companion object {
        const val PUBLIC_ACCESS_LEVEL = 0
        private const val DEFAULT_PRESIGN_URL_EXPIRY_SEC: Int = 600
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

    suspend fun getUploadPresignUrl(
        applicationId: String,
        expiry: Int = DEFAULT_PRESIGN_URL_EXPIRY_SEC,
        objectKey: String,
        fileLength: Long?,
        contentType: String,
    ): String

    suspend fun getDownloadPresignedUrl(
        bucket: String,
        objectKey: String,
        expirySeconds: Int = DEFAULT_PRESIGN_URL_EXPIRY_SEC,
        responseContentType: String? = null,
        responseContentDisposition: String? = null,
    ): String
}
