package org.woo.passport.webflux

import annotation.AuthenticationUser
import dto.UserContext
import org.springframework.core.MethodParameter
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import org.woo.mapper.Jackson
import org.woo.passport.error.PassportException
import reactor.core.publisher.Mono

class PassportReactiveResolver: HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthenticationUser::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        bindingContext: BindingContext,
        exchange: ServerWebExchange
    ): Mono<Any> {
        return extractAndSetPassport(exchange)
    }

    private fun extractAndSetPassport(exchange: ServerWebExchange): Mono<Any> {
        val passportString = exchange.request.headers.getFirst("X-User-Passport")
        return passportString?.let { str ->
            val userContext = Jackson.readValue(passportString, UserContext::class.java)
            Mono.just(userContext!!)
        } ?: Mono.empty()
    }
}