package org.woo.event.api

data class InternalApiCallEvent(
    val service: InternalService,
    val path: String,
    val statusCode: Int,
    val timestamp: Long,
    val errorMessage: String?,
    val durationMs: Long,
) {
    fun getTopic(): String = ApiEventConstants.INTERNAL_API_CALL_TOPIC
}
