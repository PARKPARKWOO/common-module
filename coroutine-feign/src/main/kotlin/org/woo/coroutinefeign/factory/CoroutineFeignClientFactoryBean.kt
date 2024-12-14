package org.woo.coroutinefeign.factory

import org.springframework.beans.factory.FactoryBean
import org.woo.coroutinefeign.invoke.CoroutineFeignInvocationHandler
import org.woo.coroutinefeign.outgoing.CoroutineFeignAdapter
import java.lang.reflect.Proxy

// Custom FactoryBean to create dynamic proxies for CoroutineFeignClient interfaces
class CoroutineFeignClientFactoryBean<T>(
    private val interfaceType: Class<T>,
    private val coroutineFeignAdapter: CoroutineFeignAdapter,
) : FactoryBean<T> {
    override fun getObject(): T =
        Proxy.newProxyInstance(
            interfaceType.classLoader,
            arrayOf(interfaceType),
        ) { _, method, args ->
            CoroutineFeignInvocationHandler(coroutineFeignAdapter).invoke(null, method, args)
        } as T

    override fun getObjectType(): Class<T> = interfaceType

    override fun isSingleton(): Boolean = true
}
