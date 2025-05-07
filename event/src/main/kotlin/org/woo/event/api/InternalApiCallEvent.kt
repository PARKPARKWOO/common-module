package org.woo.event.api

data class InternalApiCallEvent(
    val uri: String,
    val method: String,
    val isSuccess: Boolean,
    val statusCode: Int,
    val timestamp: Long,
    val errorMessage: String?,
    val durationMs: Long,
) {
    fun getTopic(): String = ApiEventConstants.INTERNAL_API_CALL_TOPIC
}
