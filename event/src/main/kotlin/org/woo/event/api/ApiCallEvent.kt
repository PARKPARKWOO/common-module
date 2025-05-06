package org.woo.event.api

abstract class ApiCallEvent(
    open val uri: String,
    open val method: String,
    open val isSuccess: Boolean,
    open val timestamp: Long,
    open val durationMs: Long,
    open val statusCode: Int,
    open val errorMessage: String?,
) {
    abstract fun getTopic(): String
}
