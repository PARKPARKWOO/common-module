package org.woo.coroutinefeign.annotation

import org.springframework.aot.hint.annotation.Reflective
import org.woo.coroutinefeign.processor.CoroutineFeignReflectiveProcessor

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Reflective(CoroutineFeignReflectiveProcessor::class)
annotation class CoroutineFeignClient(
    val serviceName: String,
)
