package org.woo.apm.log.config

import brave.Tracer
import brave.Tracing
import brave.sampler.Sampler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.WebFilter
import org.woo.apm.log.constant.ContextConstant.TRACE_ID

@Configuration
class TracingConfig(
    @Value("\${spring.application.name}")
    val applicationName: String,
) {
    companion object {
        fun create(tracer: Tracer): WebFilter =
            WebFilter { exchange, chain ->
                val currentSpan = tracer.currentSpan()
                if (currentSpan != null) {
                    exchange.response.headers.add(TRACE_ID, currentSpan.context().traceIdString())
                } else {
                    // Tracing Context가 없는 경우를 처리 (Optional: 로그 추가)
                    exchange.response.headers.add(TRACE_ID, "no-span")
                }
                chain.filter(exchange)
            }
    }

    @Bean
    fun tracing(): Tracing =
        Tracing
            .newBuilder()
            .localServiceName(applicationName)
            .sampler(Sampler.ALWAYS_SAMPLE)
            .build()
}
