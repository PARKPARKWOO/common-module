package org.woo.event.inbox

import org.woo.event.envelope.EventEnvelope
import java.time.Instant

/**
 * Consumer-side inbox storage port (P10).
 *
 * 각 서비스가 자체 DB schema(`{service}_event_inbox`)에 매핑해 구현한다 — JPA / R2DBC / Exposed /
 * raw JDBC 등 자유. 본 인터페이스는 인프라 의존성을 일절 두지 않는다.
 *
 * **트랜잭션 경계**: 본 인터페이스의 단일 메서드 호출은 caller가 잡은 트랜잭션 안에서 실행되어야
 * 한다 (Spring `@Transactional` 등). 특히 [reserve]와 비즈니스 처리 + [markProcessed]는 같은
 * 트랜잭션에서 commit되어야 exactly-once processing이 보장된다.
 *
 * **권장 schema** (참고 — 서비스 PRD/ERD에 기록 후 자체 마이그레이션):
 * ```sql
 * CREATE TABLE {service}_event_inbox (
 *   source             VARCHAR(50)  NOT NULL,
 *   event_id           VARCHAR(36)  NOT NULL,
 *   status             VARCHAR(20)  NOT NULL,  -- InboxStatus
 *   attempts           INT          NOT NULL DEFAULT 0,
 *   first_received_at  DATETIME(6)  NOT NULL,
 *   last_error         VARCHAR(500),
 *   processed_at       DATETIME(6),
 *   PRIMARY KEY (source, event_id)
 * );
 * ```
 */
interface InboxStore {

    /**
     * 멱등 reserve.
     *
     * - 처음 보는 (source, eventId)면 `PENDING` row를 insert하고 [ReserveOutcome.Reserved] 반환.
     *   caller는 비즈니스 처리 후 [markProcessed]를 호출.
     * - 이미 `PROCESSED` 상태 row가 있으면 [ReserveOutcome.AlreadyProcessed] 반환.
     *   caller는 비즈니스 로직을 건너뛰고 ack/commit만 수행.
     * - `PENDING` 또는 `FAILED` row가 있으면 [ReserveOutcome.Retryable] 반환 (재시도 케이스).
     *   caller는 재시도 정책에 따라 처리 후 [markProcessed] 또는 [markFailed].
     *
     * 구현은 unique 제약 위반을 dedup으로 활용 — `INSERT ... ON CONFLICT DO NOTHING` 또는 동등.
     */
    fun reserve(envelope: EventEnvelope, now: Instant): ReserveOutcome

    /**
     * 처리 성공 — `PROCESSED` 전이. terminal. 같은 트랜잭션에서 비즈니스 변경과 함께 commit.
     *
     * @return 실제로 갱신된 경우 true. 다른 워커가 이미 처리해 status가 PROCESSED면 false.
     */
    fun markProcessed(source: String, eventId: String, processedAt: Instant): Boolean

    /**
     * 처리 실패 — `FAILED` 전이 + attempts++ + lastError 기록. caller가 재시도 정책 결정.
     *
     * @return 실제로 갱신된 경우 true. status가 PROCESSED/DEAD면 false (terminal).
     */
    fun markFailed(source: String, eventId: String, occurredAt: Instant, error: String): Boolean

    /**
     * 재시도 임계 초과 — `DEAD` 전이. 수동 처리 큐로 이동.
     */
    fun markDead(source: String, eventId: String, occurredAt: Instant, error: String): Boolean
}

/**
 * [InboxStore.reserve] 결과 — caller가 비즈니스 처리 분기 결정에 사용.
 */
sealed class ReserveOutcome {
    abstract val record: InboxRecord

    /** 처음 본 메시지 — caller가 비즈니스 처리 후 markProcessed. */
    data class Reserved(override val record: InboxRecord) : ReserveOutcome()

    /** 이미 처리 완료 — caller는 no-op하고 ack. */
    data class AlreadyProcessed(override val record: InboxRecord) : ReserveOutcome()

    /** PENDING/FAILED 상태 (재시도) — caller가 재시도 정책에 따라 처리. */
    data class Retryable(override val record: InboxRecord) : ReserveOutcome()

    /** DEAD 상태 (terminal) — 수동 처리 대상이라 자동 재시도 금지. */
    data class Dead(override val record: InboxRecord) : ReserveOutcome()
}
