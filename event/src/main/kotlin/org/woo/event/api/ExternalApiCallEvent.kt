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
    val topic: String
        get() = ApiEventConstants.EXTERNAL_API_CALL_TOPIC
}
