package org.woo.event.inbox

import org.woo.event.envelope.EventEnvelope
import java.time.Clock
import java.time.Instant

/**
 * Inbox 처리 helper (P10) — `reserve → handler → markProcessed/markFailed` 표준 흐름을 한 곳에 묶음.
 *
 * **사용 예** (consumer 측, Spring Kafka listener):
 * ```kotlin
 * @KafkaListener(topics = ["payment.events.v1"])
 * @Transactional
 * fun onPaymentEvent(record: ConsumerRecord<String, String>) {
 *     val envelope = parseEnvelope(record)
 *     processor.process(envelope) {
 *         // 비즈니스 처리 — 같은 TX에서 commit. 예외 던지면 markFailed.
 *         entitlementCache.invalidate(envelope.payloadJson)
 *     }
 * }
 * ```
 *
 * **트랜잭션**: caller가 트랜잭션을 잡고 본 메서드를 호출해야 한다 (구현은 [InboxStore] 호출만).
 * Spring `@Transactional`로 감싸면 reserve + handler + markProcessed가 한 commit에 묶여
 * exactly-once processing이 성립.
 *
 * **재시도 정책**: handler가 예외를 던지면 본 helper는 markFailed 후 예외를 다시 throw해
 * Kafka consumer의 retry/DLQ 메커니즘에 위임. retry 한도 초과 시 [markDead]는 caller가 직접 호출.
 */
class InboxProcessor(
    private val store: InboxStore,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun <T> process(envelope: EventEnvelope, handler: () -> T): ProcessOutcome<T> {
        val now = Instant.now(clock)
        return when (val outcome = store.reserve(envelope, now)) {
            is ReserveOutcome.AlreadyProcessed -> ProcessOutcome.SkippedDuplicate(outcome.record)
            is ReserveOutcome.Dead -> ProcessOutcome.SkippedDead(outcome.record)
            is ReserveOutcome.Reserved, is ReserveOutcome.Retryable -> {
                try {
                    val result = handler()
                    store.markProcessed(envelope.source, envelope.eventId, Instant.now(clock))
                    ProcessOutcome.Processed(result)
                } catch (e: Exception) {
                    store.markFailed(
                        source = envelope.source,
                        eventId = envelope.eventId,
                        occurredAt = Instant.now(clock),
                        error = e.message ?: e::class.qualifiedName ?: "handler failed",
                    )
                    throw e
                }
            }
        }
    }
}

/**
 * [InboxProcessor.process] 결과 — caller가 메트릭/로깅에 사용.
 */
sealed class ProcessOutcome<out T> {
    data class Processed<T>(val result: T) : ProcessOutcome<T>()
    data class SkippedDuplicate(val record: InboxRecord) : ProcessOutcome<Nothing>()
    data class SkippedDead(val record: InboxRecord) : ProcessOutcome<Nothing>()
}
