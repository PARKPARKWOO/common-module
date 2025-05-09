package org.woo.storagesdk.interceptor

import org.woo.storagesdk.exception.NotAllowedMimeTypeException
import java.io.InputStream

class FileExtensionInterceptor(
    private val allowTypes: Set<AllowExtension>,
    private val order: Int = 0,
) : UploadInterceptor {
    override fun call(
        data: InputStream,
        originFileName: String,
    ): InputStream {
        val ext =
            originFileName
                .substringAfterLast('.', "")
                .lowercase()
                .let { ".$it" }

        if (allowTypes.none { it.matches(ext) }) {
            throw NotAllowedMimeTypeException("Not allowed file extension: $ext")
        }
        return data
    }

    override fun getOrder(): Int = order
}
