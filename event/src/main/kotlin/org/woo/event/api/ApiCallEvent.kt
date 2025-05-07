package org.woo.event.api

abstract class ApiCallEvent(
    open val uri: String = "",
    open val method: String = "",
    open val isSuccess: Boolean = false,
    open val timestamp: Long = 0L,
    open val durationMs: Long = 0L,
    open val statusCode: Int = 0,
    open val errorMessage: String? = null,
) {
    abstract fun getTopic(): String
}
