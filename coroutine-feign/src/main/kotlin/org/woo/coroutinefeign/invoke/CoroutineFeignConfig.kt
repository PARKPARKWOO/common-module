package org.woo.coroutinefeign.invoke

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@Import(CoroutineFeignClientRegistrar::class)
class CoroutineFeignConfig {
    @Bean
    fun defaultWebClient(): WebClient = WebClient.create()
}
