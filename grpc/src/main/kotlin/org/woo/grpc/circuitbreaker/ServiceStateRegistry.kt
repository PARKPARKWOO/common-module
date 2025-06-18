package org.woo.grpc.circuitbreaker

import io.grpc.ServiceDescriptor
import org.woo.grpc.circuitbreaker.CircuitBreakerState.CLOSED
import org.woo.grpc.circuitbreaker.CircuitBreakerState.OPEN
import org.woo.grpc.exception.InitializeFailureException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object ServiceStateRegistry {
    private val STATE_MACHINE: ConcurrentMap<String, CircuitBreakerState> = ConcurrentHashMap()

    fun initGrpcService(serviceDescriptor: ServiceDescriptor) {
        serviceDescriptor.methods.forEach { method ->
            val bareMethodName = method.bareMethodName
            bareMethodName?.let {
                STATE_MACHINE.putIfAbsent(it, CLOSED)
            }
        }
    }

    fun open(serviceName: String) {
        STATE_MACHINE[serviceName] = OPEN
    }

    fun close(serviceName: String) {
        STATE_MACHINE[serviceName] = CLOSED
    }

    fun getState(serviceName: String): CircuitBreakerState =
        STATE_MACHINE[serviceName] ?: throw InitializeFailureException("failed to get state for $serviceName")
}
