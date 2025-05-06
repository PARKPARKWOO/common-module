package org.woo.event.api

data class ApiCallMetricsEvent(
    val uri: String,
    val windowStart: Long,
    val windowEnd: Long,
    val totalCalls: Long,
    val successCalls: Long,
    val failureCalls: Long,
    val failureRate: Double,
)
