package org.woo.grpc

import org.woo.error.grpc.ErrorProto.ErrorResponse

object ErrorConverter {
    fun toGrpcErrorResponse(
        message: String,
        status: Int,
    ): ErrorResponse =
        ErrorResponse
            .newBuilder()
            .setMessage(message)
            .setStatus(status)
            .build()
}
