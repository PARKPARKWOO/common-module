package org.woo.event.api

data class InternalApiCallEvent(
    override val uri: String,
    override val method: String,
    override val isSuccess: Boolean,
    override val statusCode: Int,
    override val timestamp: Long,
    override val errorMessage: String?,
    override val durationMs: Long,
) : ApiCallEvent(
        uri = uri,
        method = method,
        isSuccess = isSuccess,
        statusCode = statusCode,
        timestamp = timestamp,
        errorMessage = errorMessage,
        durationMs = durationMs,
    ) {
    override fun getTopic(): String = ApiEventConstants.INTERNAL_API_CALL_TOPIC
}
