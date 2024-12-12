package org.woo.coroutinefeign.invoke

import com.netflix.discovery.EurekaClient
import org.reflections.Reflections
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.function.client.WebClient
import org.woo.coroutinefeign.annotation.CoroutineFeignClient
import org.woo.coroutinefeign.outgoing.CoroutineFeignAdapter
import java.lang.reflect.Proxy

class CoroutineFeignClientRegistrar : BeanDefinitionRegistryPostProcessor {
    companion object {
        private const val CLIENT_BEAN_NAME = "coroutineFeignClientInvocationHandler_"
    }

    private lateinit var applicationContext: ApplicationContext
    private lateinit var eurekaClient: EurekaClient
    private lateinit var webClient: WebClient

    fun initialize(
        applicationContext: ApplicationContext,
        eurekaClient: EurekaClient,
        webClient: WebClient,
    ) {
        this.applicationContext = applicationContext
        this.eurekaClient = eurekaClient
        this.webClient = webClient
    }

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        val basePackage = applicationContext.javaClass.packageName

        val reflections = Reflections(basePackage)
        val candidates = reflections.getTypesAnnotatedWith(CoroutineFeignClient::class.java)

        for (candidate in candidates) {
            if (candidate.isInterface) {
                val proxy =
                    Proxy.newProxyInstance(
                        candidate.classLoader,
                        arrayOf(candidate),
                    ) { proxy, method, args ->
                        CoroutineFeignInvocationHandler(createAdapter()).invoke(proxy, method, args)
                    } as Any

                val builder =
                    BeanDefinitionBuilder.genericBeanDefinition(candidate as Class<Any>) {
                        proxy
                    }
                registry.registerBeanDefinition(CLIENT_BEAN_NAME + candidate.simpleName, builder.beanDefinition)
            }
        }
    }

    private fun createAdapter(): CoroutineFeignAdapter = CoroutineFeignAdapter(eurekaClient, webClient)

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        // No-op
    }
}
