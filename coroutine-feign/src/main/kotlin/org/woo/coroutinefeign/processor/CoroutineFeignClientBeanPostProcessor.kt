package org.woo.coroutinefeign.processor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.woo.coroutinefeign.annotation.CoroutineFeignClient
import org.woo.coroutinefeign.factory.CoroutineFeignClientFactoryBean
import org.woo.coroutinefeign.outgoing.CoroutineFeignAdapter

@Configuration
@Import(CoroutineFeignAdapter::class)
class CoroutineFeignClientBeanPostProcessor
    @Autowired
    constructor(
        private val coroutineFeignAdapter: CoroutineFeignAdapter,
    ) : BeanPostProcessor {
        override fun postProcessAfterInitialization(
            bean: Any,
            beanName: String,
        ): Any {
            val beanClass = bean.javaClass

            // Check if the bean is an interface with CoroutineFeignClient annotation
            if (beanClass.isInterface && beanClass.isAnnotationPresent(CoroutineFeignClient::class.java)) {
                return CoroutineFeignClientFactoryBean(beanClass, coroutineFeignAdapter).getObject()
            }

            return bean
        }
    }
