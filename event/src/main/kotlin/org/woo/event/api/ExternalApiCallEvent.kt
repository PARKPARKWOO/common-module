package org.woo.event.api

data class ExternalApiCallEvent(
    val baseUrl: String,
    val path: String,
    val method: String,
    val isSuccess: Boolean,
    val statusCode: Int,
    val timestamp: Long,
    val errorMessage: String?,
    val durationMs: Long,
) {
    fun getTopic(): String = ApiEventConstants.EXTERNAL_API_CALL_TOPIC
}
