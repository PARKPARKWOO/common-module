package org.woo.storagesdk

import io.grpc.Context
import io.grpc.Metadata

object FileMetadata {
    val File_METADATA_KEY: Metadata.Key<String> =
        Metadata.Key.of("file_metadata", Metadata.ASCII_STRING_MARSHALLER)
    val FILE_METADATA_CONTEXT_KEY: Context.Key<String> = Context.key("file_metadata")
}
