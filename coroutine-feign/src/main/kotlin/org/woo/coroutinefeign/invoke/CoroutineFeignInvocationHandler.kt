package org.woo.coroutinefeign.invoke

import kotlinx.coroutines.runBlocking
import org.springframework.cglib.proxy.InvocationHandler
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.woo.coroutinefeign.annotation.CoroutineFeignClient
import org.woo.coroutinefeign.annotation.Delete
import org.woo.coroutinefeign.annotation.Get
import org.woo.coroutinefeign.annotation.Patch
import org.woo.coroutinefeign.annotation.Post
import org.woo.coroutinefeign.annotation.Put
import org.woo.coroutinefeign.outgoing.CoroutineFeignAdapter
import java.lang.reflect.Method

class CoroutineFeignInvocationHandler(
    private val adapter: CoroutineFeignAdapter,
) : InvocationHandler {
    override fun invoke(
        proxy: Any?,
        method: Method,
        args: Array<out Any>?,
    ): Any {
        val httpMethod =
            when {
                method.isAnnotationPresent(Get::class.java) -> "GET"
                method.isAnnotationPresent(Post::class.java) -> "POST"
                method.isAnnotationPresent(Put::class.java) -> "PUT"
                method.isAnnotationPresent(Patch::class.java) -> "PATCH"
                method.isAnnotationPresent(Delete::class.java) -> "DELETE"
                else -> throw UnsupportedOperationException("Unsupported HTTP method for: ${method.name}")
            }

        val path =
            method.getAnnotation(Get::class.java)?.path
                ?: method.getAnnotation(Post::class.java)?.path
                ?: method.getAnnotation(Put::class.java)?.path
                ?: method.getAnnotation(Patch::class.java)?.path
                ?: method.getAnnotation(Delete::class.java)?.path
                ?: throw IllegalArgumentException("Path must be specified in the annotation.")

        val serviceName =
            method.declaringClass.getAnnotation(CoroutineFeignClient::class.java)?.serviceName
                ?: throw IllegalArgumentException("Service name must be specified in @CoroutineFeignClient.")

        val headers = mutableMapOf<String, String>()
        val body: Any? = extractBody(method, args)

        method.parameters.forEachIndexed { index, parameter ->
            val headerAnnotation = parameter.getAnnotation(RequestHeader::class.java)
            if (headerAnnotation != null && args != null) {
                val headerName = headerAnnotation.value.ifBlank { parameter.name ?: "" }
                headers[headerName] = args[index].toString()
            }
        }

        return runBlocking {
            when (httpMethod) {
                "GET" ->
                    adapter.handleGetRequest(
                        appName = serviceName,
                        path = path,
                        headers = headers,
                        queryParams = extractQueryParams(method, args),
                    )

                "POST" ->
                    adapter.handlePostRequest(
                        appName = serviceName,
                        path = path,
                        headers = headers,
                        body = body,
                    )

                "PUT" ->
                    adapter.handlePutRequest(
                        appName = serviceName,
                        path = path,
                        headers = headers,
                        body = body,
                    )

                "PATCH" ->
                    adapter.handlePatchRequest(
                        appName = serviceName,
                        path = path,
                        headers = headers,
                        body = body,
                    )

                "DELETE" ->
                    adapter.handleDeleteRequest(
                        appName = serviceName,
                        path = path,
                        headers = headers,
                        body = body,
                    )

                else -> throw IllegalArgumentException("Unsupported HTTP method: $httpMethod")
            }
        }
    }

    private fun extractBody(
        method: Method,
        args: Array<out Any>?,
    ): Any? {
        method.parameters.forEachIndexed { index, parameter ->
            if (parameter.isAnnotationPresent(RequestBody::class.java)) {
                return args?.get(index)
            }
        }
        return null
    }

    private fun extractQueryParams(
        method: Method,
        args: Array<out Any>?,
    ): Map<String, String?> {
        val queryParams = mutableMapOf<String, String?>()
        method.parameters.forEachIndexed { index, parameter ->
            if (parameter.isAnnotationPresent(org.springframework.web.bind.annotation.RequestParam::class.java)) {
                val paramAnnotation =
                    parameter.getAnnotation(org.springframework.web.bind.annotation.RequestParam::class.java)
                val paramName = paramAnnotation.value.ifBlank { parameter.name ?: "" }
                queryParams[paramName] = args?.get(index)?.toString()
            }
        }
        return queryParams
    }
}
