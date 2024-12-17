package org.woo.grpc

import io.grpc.Context
import io.grpc.Metadata

object AuthMetadata {
    val AUTHORIZATION_METADATA_KEY: Metadata.Key<String> =
        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
    val JWT_TOKEN_CONTEXT_KEY: Context.Key<String> = Context.key("jwt_token")
}
