package org.woo.coroutinefeign.invoke

import com.netflix.discovery.EurekaClient
import org.reflections.Reflections
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.function.client.WebClient
import org.woo.coroutinefeign.annotation.CoroutineFeignClient
import org.woo.coroutinefeign.outgoing.CoroutineFeignAdapter
import java.lang.reflect.Proxy

class CoroutineFeignClientRegistrar(
    private val applicationContext: ApplicationContext,
    private val eurekaClient: EurekaClient,
    private val webClient: WebClient,
) : BeanDefinitionRegistryPostProcessor {
    companion object {
        private const val CLIENT_BEAN_NAME = "coroutineFeignClientInvocationHandler_"
    }

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        val basePackage = applicationContext.javaClass.packageName

        val reflections = Reflections(basePackage)
        val candidates = reflections.getTypesAnnotatedWith(CoroutineFeignClient::class.java)

        for (candidate in candidates) {
            if (candidate.isInterface) {
                val proxy = Proxy.newProxyInstance(
                    candidate.classLoader,
                    arrayOf(candidate),
                ) { proxy, method, args ->
                    CoroutineFeignInvocationHandler(createAdapter()).invoke(proxy, method, args)
                } as Any

                val builder = BeanDefinitionBuilder.genericBeanDefinition(candidate as Class<Any>) {
                    proxy
                }
                registry.registerBeanDefinition(CLIENT_BEAN_NAME + candidate.simpleName, builder.beanDefinition)
            }
        }
    }

    private fun createAdapter(): CoroutineFeignAdapter {
        return CoroutineFeignAdapter(eurekaClient, webClient)
    }

    override fun postProcessBeanFactory(beanFactory: org.springframework.beans.factory.config.ConfigurableListableBeanFactory) {
        // No-op
    }
}
