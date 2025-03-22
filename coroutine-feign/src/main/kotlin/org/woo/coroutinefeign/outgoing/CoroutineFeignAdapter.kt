package org.woo.coroutinefeign.outgoing

import com.netflix.appinfo.InstanceInfo
import com.netflix.discovery.EurekaClient
import kotlinx.coroutines.reactive.awaitSingle
import org.apache.hc.core5.http.message.MessageSupport.header
import org.springframework.web.reactive.function.client.WebClient
import org.woo.coroutinefeign.exception.NotFoundInstanceInfoException

class CoroutineFeignAdapter(
    private val eurekaClient: EurekaClient,
    private val webClient: WebClient,
) : CoroutineFeignPort {
    private suspend fun handle(
        appName: String,
        path: String,
        method: String,
        body: Any?,
        headers: Map<String, String> = emptyMap(),
    ): Any {
        val application = getApplication(appName)
            ?: throw NotFoundInstanceInfoException(
                message = "No instance found for application: $appName",
            )

        val baseUrl = "http://${application.hostName}:${application.port}$path"
        val requestBuilder = webClient.method(getHttpMethod(method)).uri { uriBuilder ->
            uriBuilder.path(baseUrl)
                .apply {
                    headers.forEach { (key, value) -> header(key, value) }
                }.build()
        }

        if (body != null) {
            requestBuilder.bodyValue(body)
        }

        return requestBuilder.retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()
    }

    private suspend fun getApplication(appName: String): InstanceInfo? =
        eurekaClient.getApplication(appName)
            .instances.firstOrNull()

    private fun getHttpMethod(method: String) = when (method.uppercase()) {
        "GET" -> org.springframework.http.HttpMethod.GET
        "POST" -> org.springframework.http.HttpMethod.POST
        "PUT" -> org.springframework.http.HttpMethod.PUT
        "PATCH" -> org.springframework.http.HttpMethod.PATCH
        "DELETE" -> org.springframework.http.HttpMethod.DELETE
        else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
    }

    override suspend fun handleGetRequest(
        appName: String,
        path: String,
        headers: Map<String, String>,
        queryParams: Map<String, String?>,
    ): Any {
        val fullPath = appendQueryParams(path, queryParams)
        return handle(appName, fullPath, "GET", null, headers)
    }

    private fun appendQueryParams(path: String, queryParams: Map<String, String?>): String {
        if (queryParams.isEmpty()) return path
        val query = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "$path?$query"
    }

    override suspend fun handlePostRequest(
        appName: String,
        headers: Map<String, String>,
        path: String,
        body: Any?,
    ): Any {
        return handle(appName, path, "POST", body, headers)
    }

    override suspend fun handlePutRequest(
        appName: String,
        headers: Map<String, String>,
        path: String,
        body: Any?,
    ): Any {
        return handle(appName, path, "PUT", body, headers)
    }

    override suspend fun handlePatchRequest(
        appName: String,
        headers: Map<String, String>,
        path: String,
        body: Any?,
    ): Any {
        return handle(appName, path, "PATCH", body, headers)
    }

    override suspend fun handleDeleteRequest(
        appName: String,
        headers: Map<String, String>,
        path: String,
        body: Any?,
    ): Any {
        return handle(appName, path, "DELETE", body, headers)
    }
}
