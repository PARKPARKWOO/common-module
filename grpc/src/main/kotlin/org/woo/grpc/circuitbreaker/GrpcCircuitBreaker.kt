package org.woo.grpc.circuitbreaker

import org.woo.grpc.exception.CircuitBreakerOpenException
import org.woo.http.CircuitBreaker

class GrpcCircuitBreaker : CircuitBreaker {
    override fun checkCircuitBreaker(serviceName: String) {
        val state = ServiceStateRegistry.getState(serviceName)
        if (state == CircuitBreakerState.OPEN) {
            throw CircuitBreakerOpenException("circuit breaker open $serviceName")
        }
    }
}
