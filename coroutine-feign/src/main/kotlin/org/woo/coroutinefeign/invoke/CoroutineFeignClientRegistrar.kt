package org.woo.coroutinefeign.invoke

import org.reflections.Reflections
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.woo.coroutinefeign.annotation.CoroutineFeignClient

@Component
class CoroutineFeignClientRegistrar(
    private val applicationContext: ApplicationContext,
) : BeanDefinitionRegistryPostProcessor {
    companion object {
        private const val CLIENT_BEAN_NAME = "coroutineFeignClientInvocationHandler_"
    }

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        val basePackage = applicationContext.javaClass.packageName

        val reflections = Reflections(basePackage)
        val candidates = reflections.getTypesAnnotatedWith(CoroutineFeignClient::class.java)

        for (candidate in candidates) {
            val builder = BeanDefinitionBuilder.genericBeanDefinition(CoroutineFeignInvocationHandler::class.java)
            registry.registerBeanDefinition(CLIENT_BEAN_NAME + candidate.simpleName, builder.beanDefinition)
        }
    }

    override fun postProcessBeanFactory(beanFactory: org.springframework.beans.factory.config.ConfigurableListableBeanFactory) {
        // No-op
    }
}
