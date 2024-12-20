package org.woo.grpc

import io.grpc.Metadata
import io.grpc.protobuf.ProtoUtils
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
}
