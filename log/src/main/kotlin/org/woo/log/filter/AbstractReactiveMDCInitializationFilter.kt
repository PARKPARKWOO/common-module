package org.woo.log.filter

import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.woo.log.constant.ContextConstant.METHOD
import org.woo.log.constant.ContextConstant.PATH
import org.woo.log.constant.ContextConstant.TRACE_ID
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.UUID

abstract class AbstractReactiveMDCInitializationFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val traceId = request.headers.getFirst(TRACE_ID) ?: UUID.randomUUID().toString()

        return chain.filter(exchange).contextWrite { context ->
            customizeContext(context, exchange).put(TRACE_ID, traceId)
                .put(PATH, request.path.toString())
                .put(METHOD, request.method.name())
        }
    }

    open fun customizeContext(context: Context, exchange: ServerWebExchange): Context {
        return context
    }
}
