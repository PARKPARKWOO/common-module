package org.woo.passport.web

import annotation.AuthenticationUser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.woo.passport.error.PassportException

class PassportResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(AuthenticationUser::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val request =
            webRequest.getNativeRequest(HttpServletRequest::class.java)
                ?: throw PassportException("Passport request not found")
        val passport = request.getAttribute("passport")
        return if (shouldAuthenticate(parameter) || passport != null) {
            passport
        } else {
            null
        }
    }

    private fun shouldAuthenticate(parameter: MethodParameter): Boolean {
        val authenticationUser = parameter.getParameterAnnotation(AuthenticationUser::class.java)
        return authenticationUser?.isRequired ?: false
    }
}