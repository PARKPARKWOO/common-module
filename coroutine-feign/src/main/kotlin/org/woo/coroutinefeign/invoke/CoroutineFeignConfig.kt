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
        val coroutineFeignClientRegistrar = CoroutineFeignClientRegistrar()
        coroutineFeignClientRegistrar.initialize(
            applicationContext = applicationContext,
            eurekaClient = eurekaClient,
            webClient = webClient,
        )
        return coroutineFeignClientRegistrar
    }

    @Bean
    fun defaultWebClient(): WebClient = WebClient.create()
}
