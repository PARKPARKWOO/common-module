package org.woo.storagesdk.interceptor

import org.springframework.core.Ordered
import java.io.InputStream

interface UploadInterceptor : Ordered {
    fun call(
        data: InputStream,
        originFileName: String,
    ): InputStream
}
