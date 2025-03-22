package org.woo.coroutinefeign.annotation

import org.springframework.context.annotation.Import
import org.woo.coroutinefeign.invoke.CoroutineFeignConfig

@Import(CoroutineFeignConfig::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnableCoroutineFeign
