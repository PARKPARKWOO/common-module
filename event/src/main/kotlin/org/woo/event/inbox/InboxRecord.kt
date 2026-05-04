package org.woo.event.inbox

import java.time.Instant

/**
 * Consumer inbox row 스냅샷 (P10).
 *
 * 각 서비스가 자체 schema(`{service}_event_inbox`)에 매핑하지만 도메인 의미는 본 record로 통일.
 * JPA·R2DBC·Exposed 등 어떤 매핑이든 [InboxStore] 구현이 본 record 형태로 노출하면 됨.
 */
data class InboxRecord(
    val source: String,
    val eventId: String,
    val status: InboxStatus,
    val attempts: Int,
    val firstReceivedAt: Instant,
    val lastError: String? = null,
    val processedAt: Instant? = null,
) {
    init {
        require(source.isNotBlank()) { "source must not be blank" }
        require(eventId.isNotBlank()) { "eventId must not be blank" }
        require(attempts >= 0) { "attempts must be >= 0 (got $attempts)" }
    }

    fun isProcessed(): Boolean = status == InboxStatus.PROCESSED
    fun isTerminal(): Boolean = status == InboxStatus.PROCESSED || status == InboxStatus.DEAD
}
