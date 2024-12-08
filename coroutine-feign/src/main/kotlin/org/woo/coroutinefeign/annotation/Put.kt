package org.woo.coroutinefeign.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Put(
    val path: String,
)
