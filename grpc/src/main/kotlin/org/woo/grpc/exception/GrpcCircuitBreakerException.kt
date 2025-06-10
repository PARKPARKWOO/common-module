package org.woo.grpc.exception

import org.woo.http.CircuitBreakerException

open class GrpcCircuitBreakerException(
    override val message: String,
) : CircuitBreakerException(message)

data class InitializeFailureException(
    override val message: String,
) : GrpcCircuitBreakerException(message)

data class CircuitBreakerOpenException(
    override val message: String,
) : GrpcCircuitBreakerException(message)
