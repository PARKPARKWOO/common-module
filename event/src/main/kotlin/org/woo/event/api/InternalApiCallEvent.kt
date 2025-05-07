package org.woo.event.api

data class InternalApiCallEvent(
    val serviceName: String,
    val path: String,
    val statusCode: Int,
    val timestamp: Long,
    val errorMessage: String?,
    val durationMs: Long,
) {
    fun getTopic(): String = ApiEventConstants.INTERNAL_API_CALL_TOPIC
}
