package org.woo.event.api

data class InternalApiCallEvent(
    val serviceName: String,
    val statusCode: Int,
    val timestamp: Long,
    val errorMessage: String?,
    val durationMs: Long,
) {
    val topic: String
        get() = ApiEventConstants.INTERNAL_API_CALL_TOPIC
}
