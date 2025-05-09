package org.woo.storagesdk.exception

open class FileUploadException(
    override val message: String,
) : RuntimeException(message)

data class MaxChunkSizeExceededException(
    override val message: String,
) : FileUploadException(message)

data class InvalidArgumentException(
    override val message: String,
) : FileUploadException(message)

data class ResourceExhaustedException(
    override val message: String,
) : FileUploadException(message)

data class ServiceUnavailableException(
    override val message: String,
) : FileUploadException(message)

data class TimeoutException(
    override val message: String,
) : FileUploadException(message)

data class PermissionDeniedException(
    override val message: String,
) : FileUploadException(message)
