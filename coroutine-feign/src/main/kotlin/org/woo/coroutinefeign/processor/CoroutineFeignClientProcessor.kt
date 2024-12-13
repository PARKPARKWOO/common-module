package org.woo.coroutinefeign.processor

import com.netflix.discovery.EurekaClient
import org.reflections.Reflections
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.function.client.WebClient
import org.woo.coroutinefeign.annotation.CoroutineFeignClient
import org.woo.coroutinefeign.invoke.CoroutineFeignInvocationHandler
import org.woo.coroutinefeign.outgoing.CoroutineFeignAdapter
import java.lang.reflect.Proxy

class CoroutineFeignClientProcessor(
    private val applicationContext: ApplicationContext,
    private val eurekaClient: EurekaClient,
    private val webClient: WebClient,
) : BeanPostProcessor {
    override fun postProcessAfterInitialization(
        bean: Any,
        beanName: String,
    ): Any {
        val basePackage = applicationContext.javaClass.packageName
        val reflections = Reflections(basePackage)
        val candidates = reflections.getTypesAnnotatedWith(CoroutineFeignClient::class.java)

        candidates.forEach { candidate ->
            if (candidate.isInterface && bean.javaClass == candidate) {
                return Proxy.newProxyInstance(
                    candidate.classLoader,
                    arrayOf(candidate),
                ) { proxy, method, args ->
                    CoroutineFeignInvocationHandler(createAdapter()).invoke(proxy, method, args)
                } as Any
            }
        }

        return bean
    }

    private fun createAdapter(): CoroutineFeignAdapter = CoroutineFeignAdapter(eurekaClient, webClient)
}
