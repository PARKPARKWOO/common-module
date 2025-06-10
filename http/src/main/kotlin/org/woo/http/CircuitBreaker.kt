package org.woo.http

interface CircuitBreaker {
    @Throws(CircuitBreakerException::class)
    fun checkCircuitBreaker(url: String)

    @Throws(CircuitBreakerException::class)
    fun <T> requireCircuitClosed(
        url: String,
        block: () -> T,
    ): T {
        checkCircuitBreaker(url)
        return block()
    }
}
