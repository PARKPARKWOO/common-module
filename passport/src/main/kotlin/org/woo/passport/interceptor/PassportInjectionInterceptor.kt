package org.woo.passport.interceptor

import annotation.PublicEndPoint
import dto.UserContext
import org.springframework.http.HttpMethod
import org.springframework.web.method.HandlerMethod
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.woo.mapper.Jackson
import reactor.core.publisher.Mono

class PassportInjectionInterceptor: WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response

        // OPTIONS 메서드는 바로 다음 필터로 전달
        if (request.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }

        // PublicEndPoint 어노테이션이 있는지 확인
        val isPublicEndPoint = exchange.getAttribute<HandlerMethod>("org.springframework.web.reactive.HandlerMapping.bestMatchingHandler")
            ?.hasMethodAnnotation(PublicEndPoint::class.java) ?: false

        return if (isPublicEndPoint) {
            // PublicEndPoint인 경우 바로 다음 필터로 전달
            chain.filter(exchange)
        } else {
            // 인증 및 사용자 정보 설정
            extractAndSetPassport(exchange)
                .flatMap { chain.filter(exchange) }
        }
    }

    private fun extractAndSetPassport(exchange: ServerWebExchange): Mono<Void> {
        val passportString = exchange.request.headers.getFirst("X-User-Passport")
        return if (passportString != null) {
            val userContext = Jackson.readValue(passportString, UserContext::class.java)
            exchange.attributes["passport"] = userContext
            Mono.empty()
        } else {
            // Passport 헤더가 없는 경우 처리 로직 추가 가능
            Mono.empty()
        }
    }
}