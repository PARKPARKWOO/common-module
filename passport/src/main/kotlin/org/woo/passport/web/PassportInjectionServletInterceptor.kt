package org.woo.passport.web

import annotation.PublicEndPoint
import dto.UserContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import org.woo.mapper.Jackson

class PassportInjectionServletInterceptor: HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method.equals(HttpMethod.OPTIONS.name())) {
            return true
        }
        val handlerMethod = handler as? HandlerMethod
        return initializeTokenIfNeeded(request, handlerMethod)
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        super.postHandle(request, response, handler, modelAndView)
    }
    private fun initializeTokenIfNeeded(
        request: HttpServletRequest,
        handlerMethod: HandlerMethod?,
    ): Boolean =
        runCatching {
            val passport = request.getPassport()
            request.setAttribute("passport", passport)
            true
        }.getOrElse {
            val publicEndPoint = handlerMethod?.hasMethodAnnotation(PublicEndPoint::class.java) ?: false
            if (publicEndPoint) {
                true
            } else {
                throw it
            }
        }
    private fun HttpServletRequest.getPassport(): UserContext? {
        val passportString = this.getHeader("X-User-Passport")
        return Jackson.readValue(passportString, UserContext::class.java)
    }
}