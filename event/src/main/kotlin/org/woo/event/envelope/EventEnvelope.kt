package org.woo.event.envelope

import java.time.Instant

/**
 * Cross-service event envelope (P10 표준).
 *
 * 발행 측은 outbox row insert 시점에 [eventId]를 자체 생성(UUID v4 권장)해 envelope에 포함하고,
 * Kafka로 publish할 때 메시지 헤더 또는 body에 그대로 흘려보낸다. consumer는 [InboxStore]에
 * `(source, eventId)` 키로 dedup-insert해 exactly-once processing을 달성한다.
 *
 * 본 클래스는 어떤 인프라 의존성도 두지 않는다 — JPA·Spring·Kafka 클라이언트 모두 caller 책임.
 *
 * @property source 발행 서비스 namespace. 예: `"payment"`, `"billing"`, `"bbr-backend"`.
 * @property eventId 메시지 정체성 UUID. 같은 비즈니스 이벤트의 재발행은 같은 [eventId]를 유지해
 *   consumer가 두 번째 메시지를 멱등하게 무시할 수 있게 한다.
 * @property eventType 비즈니스 이벤트 타입. 예: `"ORDER_PAID"`, `"ENTITLEMENT_GRANTED"`.
 * @property payloadJson 이벤트 본문 (JSON 문자열). 직렬화 포맷은 caller 합의.
 * @property occurredAt 비즈니스 발생 시각 (UTC). publish 시각이 아니라 도메인 이벤트 발생 시각.
 */
data class EventEnvelope(
    val source: String,
    val eventId: String,
    val eventType: String,
    val payloadJson: String,
    val occurredAt: Instant,
) {
    init {
        require(source.isNotBlank()) { "source must not be blank" }
        require(eventId.isNotBlank()) { "eventId must not be blank" }
        require(eventType.isNotBlank()) { "eventType must not be blank" }
        require(payloadJson.isNotBlank()) { "payloadJson must not be blank" }
    }
}
