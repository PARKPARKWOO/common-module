package org.woo.event.api

data class ExternalApiCallEvent(
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
        statusCode = statusCode,
        isSuccess = isSuccess,
        errorMessage = errorMessage,
        timestamp = timestamp,
        durationMs = durationMs,
    ) {
    override fun getTopic(): String = ApiEventConstants.EXTERNAL_API_CALL_TOPIC
}
