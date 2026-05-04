package org.woo.event.inbox

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.woo.event.envelope.EventEnvelope
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * InboxProcessor — P10 exactly-once processing 검증.
 *
 * 핵심 invariants:
 *  - 같은 (source, eventId) 두 번 처리 시 두 번째는 SkippedDuplicate (handler 호출 안 함)
 *  - handler 예외 시 markFailed 호출 + 예외 re-throw
 *  - DEAD 상태는 SkippedDead (자동 재시도 안 함)
 *  - Retryable 상태는 handler 호출
 */
class InboxProcessorTest {

    private val now: Instant = Instant.parse("2026-05-04T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun envelope(eventId: String = "evt-1") = EventEnvelope(
        source = "payment",
        eventId = eventId,
        eventType = "ORDER_PAID",
        payloadJson = """{"orderId":"o-1"}""",
        occurredAt = now,
    )

    @Test
    fun `처음 본 메시지는 handler 호출 후 markProcessed`() {
        val store = InMemoryInboxStore()
        val processor = InboxProcessor(store, clock)
        var handlerCalls = 0

        val outcome = processor.process(envelope()) { handlerCalls++; "ok" }

        assertEquals(1, handlerCalls)
        assertTrue(outcome is ProcessOutcome.Processed && outcome.result == "ok")
        val record = store.find("payment", "evt-1")!!
        assertEquals(InboxStatus.PROCESSED, record.status)
        assertEquals(now, record.processedAt)
    }

    @Test
    fun `같은 envelope 두번째 호출은 SkippedDuplicate — handler 호출 안 함`() {
        val store = InMemoryInboxStore()
        val processor = InboxProcessor(store, clock)
        var handlerCalls = 0

        processor.process(envelope()) { handlerCalls++ }
        val outcome = processor.process(envelope()) { handlerCalls++ }

        assertEquals(1, handlerCalls)   // 두번째는 호출 X
        assertTrue(outcome is ProcessOutcome.SkippedDuplicate)
    }

    @Test
    fun `handler 예외 시 markFailed + 예외 re-throw`() {
        val store = InMemoryInboxStore()
        val processor = InboxProcessor(store, clock)

        val ex = assertThrows(IllegalStateException::class.java) {
            processor.process(envelope()) { throw IllegalStateException("boom") }
        }
        assertEquals("boom", ex.message)

        val record = store.find("payment", "evt-1")!!
        assertEquals(InboxStatus.FAILED, record.status)
        assertEquals(1, record.attempts)
        assertEquals("boom", record.lastError)
    }

    @Test
    fun `Retryable 상태 (이전 FAILED) 는 handler 다시 호출`() {
        val store = InMemoryInboxStore()
        val processor = InboxProcessor(store, clock)

        // 1차 실패
        runCatching {
            processor.process(envelope()) { throw RuntimeException("first") }
        }

        // 2차 시도 — Retryable이라 handler 다시 호출, 성공
        val outcome = processor.process(envelope()) { "second-ok" }

        assertTrue(outcome is ProcessOutcome.Processed)
        assertEquals(InboxStatus.PROCESSED, store.find("payment", "evt-1")!!.status)
    }

    @Test
    fun `DEAD 상태는 SkippedDead — handler 호출 안 함`() {
        val store = InMemoryInboxStore()
        val processor = InboxProcessor(store, clock)
        store.markDead("payment", "evt-1", now, "permanent")
        // markDead는 reserve 없이 직접 호출하면 record가 없으므로 먼저 reserve
        store.reserve(envelope(), now)
        store.markDead("payment", "evt-1", now, "permanent")

        var handlerCalls = 0
        val outcome = processor.process(envelope()) { handlerCalls++ }

        assertEquals(0, handlerCalls)
        assertTrue(outcome is ProcessOutcome.SkippedDead)
    }

    @Test
    fun `다른 source 같은 eventId는 별개로 처리됨`() {
        val store = InMemoryInboxStore()
        val processor = InboxProcessor(store, clock)
        var calls = 0

        processor.process(envelope("dup-id").copy(source = "payment")) { calls++ }
        processor.process(envelope("dup-id").copy(source = "billing")) { calls++ }

        assertEquals(2, calls)
        assertSame(InboxStatus.PROCESSED, store.find("payment", "dup-id")!!.status)
        assertSame(InboxStatus.PROCESSED, store.find("billing", "dup-id")!!.status)
    }
}

/**
 * Test-only in-memory InboxStore — 실제 서비스는 JPA/JDBC로 구현.
 * `(source, event_id)` PK 시뮬레이션 + atomic reserve.
 */
private class InMemoryInboxStore : InboxStore {
    private val rows = mutableMapOf<Pair<String, String>, InboxRecord>()

    fun find(source: String, eventId: String): InboxRecord? = rows[source to eventId]

    @Synchronized
    override fun reserve(envelope: EventEnvelope, now: Instant): ReserveOutcome {
        val key = envelope.source to envelope.eventId
        val existing = rows[key]
        if (existing == null) {
            val record = InboxRecord(
                source = envelope.source,
                eventId = envelope.eventId,
                status = InboxStatus.PENDING,
                attempts = 0,
                firstReceivedAt = now,
            )
            rows[key] = record
            return ReserveOutcome.Reserved(record)
        }
        return when (existing.status) {
            InboxStatus.PROCESSED -> ReserveOutcome.AlreadyProcessed(existing)
            InboxStatus.DEAD -> ReserveOutcome.Dead(existing)
            InboxStatus.PENDING, InboxStatus.FAILED -> ReserveOutcome.Retryable(existing)
        }
    }

    @Synchronized
    override fun markProcessed(source: String, eventId: String, processedAt: Instant): Boolean {
        val key = source to eventId
        val existing = rows[key] ?: return false
        if (existing.status == InboxStatus.PROCESSED) return false
        rows[key] = existing.copy(status = InboxStatus.PROCESSED, processedAt = processedAt)
        return true
    }

    @Synchronized
    override fun markFailed(source: String, eventId: String, occurredAt: Instant, error: String): Boolean {
        val key = source to eventId
        val existing = rows[key] ?: return false
        if (existing.isTerminal()) return false
        rows[key] = existing.copy(
            status = InboxStatus.FAILED,
            attempts = existing.attempts + 1,
            lastError = error,
        )
        return true
    }

    @Synchronized
    override fun markDead(source: String, eventId: String, occurredAt: Instant, error: String): Boolean {
        val key = source to eventId
        val existing = rows[key] ?: return false
        if (existing.isTerminal()) return false
        rows[key] = existing.copy(status = InboxStatus.DEAD, lastError = error)
        return true
    }
}
