package org.woo.storagesdk.exception

open class InterceptorException(
    override val message: String,
) : RuntimeException(message)

data class NotAllowedMimeTypeException(
    override val message: String,
) : InterceptorException(message)
