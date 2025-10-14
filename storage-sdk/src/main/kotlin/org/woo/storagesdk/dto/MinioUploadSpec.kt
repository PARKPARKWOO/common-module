package org.woo.storagesdk.dto

import java.io.InputStream

data class MinioUploadSpec(
    val header: MinioUploadHeader,
    val data: InputStream,
)

data class MinioUploadHeader(
    val objectKey: String,
    val contentType: String?,
    val contentDisposition: String?,
    val contentLength: Int,
    val metadata: Map<String, String>?,
    val uploadedBy: String,
    val applicationId: String,
)

data class MinioUploadResponse(
    val bucket: String,
    val objectKey: String,
    val size: Long,
    val etag: String,
)
