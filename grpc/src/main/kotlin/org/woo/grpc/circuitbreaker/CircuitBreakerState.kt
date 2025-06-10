package org.woo.grpc.circuitbreaker

enum class CircuitBreakerState {
    OPEN,
    CLOSED,
    // TODO: HALF_OPEN
}
