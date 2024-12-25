package org.woo.apm.log.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import org.woo.apm.log.constant.ContextConstant.METHOD
import org.woo.apm.log.constant.ContextConstant.PATH
import org.woo.apm.log.constant.ContextConstant.TRACE_ID
import java.util.UUID

abstract class AbstractMDCInitializationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            // MDC 초기화
            initializeMdcContext(request)
            // 사용자 정의 추가 작업 (옵션)
            customizeRequest(request)
            // 다음 필터로 전달
            filterChain.doFilter(request, response)
        } finally {
            clearMdcContext()
        }
    }

    // 필요할 시 구현
    protected open fun customizeRequest(request: HttpServletRequest) {
    }

    private fun initializeMdcContext(httpRequest: HttpServletRequest) {
        var traceId = httpRequest.getHeader(TRACE_ID)

        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString()
        }

        MDC.put(TRACE_ID, traceId)
        MDC.put(METHOD, httpRequest.method)
        MDC.put(PATH, httpRequest.requestURI)
    }

    private fun clearMdcContext() {
        MDC.remove(TRACE_ID)
        MDC.remove(METHOD)
        MDC.remove(PATH)
    }
}
