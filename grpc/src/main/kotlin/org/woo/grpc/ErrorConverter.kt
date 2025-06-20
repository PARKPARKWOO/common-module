package org.woo.grpc

import io.grpc.Metadata
import io.grpc.protobuf.ProtoUtils
import org.woo.error.grpc.ErrorProto.ErrorResponse

object ErrorConverter {
    fun toGrpcErrorResponse(
        message: String,
        status: Int,
        isGrpcCode: Boolean = false,
    ): ErrorResponse {
        val statusCode =
            if (!isGrpcCode) {
                status.toGrpcStatusCode()
            } else {
                status
            }
        return ErrorResponse
            .newBuilder()
            .setMessage(message)
            .setStatus(statusCode)
            .build()
    }

    fun attachErrorToMetadata(
        error: ErrorResponse,
        data: Metadata?,
    ): Metadata {
        val metadata = data ?: Metadata()
        val keyForProto =
            ProtoUtils
                .keyForProto(ErrorResponse.getDefaultInstance())
        metadata.put(keyForProto, error)
        return metadata
    }

    fun attachErrorToMetadata(error: ErrorResponse): Metadata = attachErrorToMetadata(error, null)

    private fun Int.toGrpcStatusCode(): Int =
        when (this) {
            200 -> 0
            499 -> 1
            400 -> 3
            504 -> 4
            404 -> 5
            409 -> 6
            403 -> 7
            429 -> 8
            501 -> 12
            500 -> 13
            503 -> 14
            401 -> 16
            else -> 2
        }
}
