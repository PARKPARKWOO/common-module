package org.woo.coroutinefeign.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CoroutineFeignClient(
    val serviceName: String,
)
