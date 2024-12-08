package org.woo.coroutinefeign.outgoing

interface CoroutineFeignPort {
    suspend fun handleGetRequest(appName: String, path: String, headers: Map<String, String>, queryParams: Map<String, String?>): Any

    suspend fun handlePostRequest(appName: String, headers: Map<String, String>, path: String, body: Any?): Any

    suspend fun handlePutRequest(appName: String, headers: Map<String, String>, path: String, body: Any?): Any

    suspend fun handlePatchRequest(appName: String, headers: Map<String, String>, path: String, body: Any?): Any

    suspend fun handleDeleteRequest(appName: String, headers: Map<String, String>, path: String, body: Any?): Any
}
