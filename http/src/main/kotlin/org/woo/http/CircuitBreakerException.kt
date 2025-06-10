package org.woo.http

open class CircuitBreakerException(
    override val message: String,
) : RuntimeException(message)
