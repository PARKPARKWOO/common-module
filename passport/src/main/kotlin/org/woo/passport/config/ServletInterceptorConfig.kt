package org.woo.passport.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerInterceptor
import org.woo.passport.interceptor.PassportInjectionServletInterceptor

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class ServletInterceptorConfig {
    @Bean
    fun passportInjectionServletInterceptor(): HandlerInterceptor = PassportInjectionServletInterceptor()
}