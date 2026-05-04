package org.woo.event.inbox

/**
 * Consumer-side inbox row 상태 (P10).
 *
 * 정상 흐름: PENDING → PROCESSED.
 * 실패 누적 시 PENDING → FAILED → (재시도 후) PROCESSED 또는 영구 실패 시 DEAD.
 */
enum class InboxStatus {
    /** 신규 reserve 후 비즈니스 처리 대기. retry 도중도 동일. */
    PENDING,

    /** 비즈니스 처리 완료 — terminal. */
    PROCESSED,

    /** 처리 시도 실패 — caller 정책에 따라 재시도 가능. */
    FAILED,

    /** 재시도 임계 초과 — 운영 개입 필요 (terminal, 수동 처리). */
    DEAD,
}
