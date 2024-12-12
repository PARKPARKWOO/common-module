package org.woo.coroutinefeign.invoke

import com.netflix.discovery.EurekaClient
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class CoroutineFeignConfig {
    lateinit var eurekaClient: EurekaClient

    @Bean
    fun coroutineFeignClientRegistrar(
        applicationContext: ApplicationContext,
        webClient: WebClient,
    ): CoroutineFeignClientRegistrar {
        val eurekaClient = applicationContext.getBean(EurekaClient::class.java)
        return CoroutineFeignClientRegistrar(applicationContext, eurekaClient, webClient)
    }

    @Bean
    fun defaultWebClient(): WebClient = WebClient.create()
}
