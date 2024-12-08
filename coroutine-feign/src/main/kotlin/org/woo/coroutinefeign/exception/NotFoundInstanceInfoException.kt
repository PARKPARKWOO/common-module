package org.woo.coroutinefeign.exception

class NotFoundInstanceInfoException(
    override val message: String
) : RuntimeException(message)
