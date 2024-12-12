package org.woo.coroutinefeign.invoke

import com.netflix.discovery.EurekaClient
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class CoroutineFeignConfig {
    @Bean
    fun coroutineFeignClientRegistrar(
        applicationContext: ApplicationContext,
        webClient: WebClient,
    ): CoroutineFeignClientProcessor {
        val eurekaClient = applicationContext.getBean(EurekaClient::class.java)
        return CoroutineFeignClientProcessor(
            applicationContext = applicationContext,
            eurekaClient = eurekaClient,
            webClient = webClient,
        )
    }

    @Bean
    fun defaultWebClient(): WebClient = WebClient.create()
}
