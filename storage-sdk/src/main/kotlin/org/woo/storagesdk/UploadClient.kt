package org.woo.storagesdk

import java.io.InputStream

interface UploadClient {
    // id 값 리턴
    suspend fun uploadStream(
        fileOriginName: String,
        uploadedBy: String,
        contentLength: Long,
        chunkSize: Int,
        applicationId: Long,
        data: InputStream,
    ): Long
}
