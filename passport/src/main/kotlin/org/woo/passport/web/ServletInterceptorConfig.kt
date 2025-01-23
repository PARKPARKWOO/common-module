package org.woo.passport.web

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class ServletInterceptorConfig: WebMvcConfigurer {
    @Bean
    fun passportInjectionServletInterceptor(): HandlerInterceptor = PassportInjectionServletInterceptor()

    @Bean
    fun passportResolver(): HandlerMethodArgumentResolver = PassportResolver()

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(passportResolver())
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(passportInjectionServletInterceptor())
            .addPathPatterns("/api/**")
    }
}