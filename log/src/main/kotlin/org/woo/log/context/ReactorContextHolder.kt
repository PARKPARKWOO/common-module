package org.woo.log.context

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.withContext
import org.woo.log.constant.ContextConstant
import reactor.core.publisher.Mono
import reactor.util.context.Context

object ReactorContextHolder {
    suspend fun getContext(): Context? =
        kotlin.coroutines.coroutineContext[ReactorContext]
            ?.context

    suspend inline fun isMobileDevice(): Boolean = getContext()?.getOrDefault(ContextConstant.IS_MOBILE, false) as Boolean

    suspend inline fun getTraceId(): String {
        return getContext()?.getOrDefault(ContextConstant.TRACE_ID, "unknown") ?: ""
    }
}

suspend fun <T> withReactorContext(block: suspend () -> T): T {
    val reactorContext = Mono.deferContextual { Mono.just(it) }.awaitFirstOrNull()
    return if (reactorContext != null) {
        withContext(ReactorContext(reactorContext)) {
            block()
        }
    } else {
        block()
    }
}
