package org.woo.storagesdk.dto

import java.io.InputStream

data class MinioUploadSpec(
    val header: MinioUploadHeader,
    val data: InputStream,
)

data class MinioUploadHeader(
    val objectKey: String,
    val contentType: String? = null,
    val contentDisposition: String? = null,
    val contentLength: Int,
    val metadata: Map<String, String>? = null,
    val uploadedBy: String,
    val applicationId: String,
)

data class MinioUploadResponse(
    val bucket: String,
    val objectKey: String,
    val size: Long,
    val etag: String,
)
