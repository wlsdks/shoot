package com.stark.shoot.adapter.out.persistence.postgres.entity

import com.stark.shoot.domain.saga.SagaState
import jakarta.persistence.*
import java.time.Instant

/**
 * Outbox Dead Letter Queue (DLQ) Entity
 *
 * 재시도 횟수를 초과한 실패 이벤트를 저장하는 테이블입니다.
 *
 * **목적:**
 * - 영구적으로 실패한 이벤트를 별도 저장
 * - 운영자가 수동으로 확인 및 재처리 가능
 * - 실패 원인 분석 및 디버깅
 * - Slack 알림으로 즉시 문제 인지
 *
 * **사용 시나리오:**
 * 1. OutboxEventProcessor가 재시도 5회 초과
 * 2. 이벤트를 DLQ로 이동
 * 3. Slack 알림 전송
 * 4. 운영자가 DLQ 확인 후 수동 재처리
 *
 * **Netflix/Uber 패턴:**
 * - 모든 프로덕션 시스템은 DLQ 필수
 * - 자동 재시도 + 수동 재처리 조합
 * - 실패 이벤트 유실 방지
 */
@Entity
@Table(
    name = "outbox_dead_letter",
    indexes = [
        Index(name = "idx_dlq_saga_id", columnList = "saga_id"),
        Index(name = "idx_dlq_created_at", columnList = "created_at"),
        Index(name = "idx_dlq_resolved", columnList = "resolved")
    ]
)
class OutboxDeadLetterEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * 원본 Outbox 이벤트 ID
     */
    @Column(nullable = false, name = "original_event_id")
    val originalEventId: Long,

    /**
     * Saga ID (추적용)
     */
    @Column(nullable = false, length = 100, name = "saga_id")
    val sagaId: String,

    /**
     * Saga 최종 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "saga_state")
    val sagaState: SagaState,

    /**
     * 이벤트 타입 (클래스명)
     */
    @Column(nullable = false, length = 255, name = "event_type")
    val eventType: String,

    /**
     * 이벤트 페이로드 (JSON)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    /**
     * 실패 원인
     */
    @Column(nullable = false, columnDefinition = "TEXT", name = "failure_reason")
    val failureReason: String,

    /**
     * 재시도 횟수
     */
    @Column(nullable = false, name = "failure_count")
    val failureCount: Int,

    /**
     * 마지막 실패 시간
     */
    @Column(nullable = false, name = "last_failure_at")
    val lastFailureAt: Instant,

    /**
     * DLQ 생성 시간
     */
    @Column(nullable = false, name = "created_at")
    val createdAt: Instant = Instant.now(),

    /**
     * 해결 여부
     */
    @Column(nullable = false)
    var resolved: Boolean = false,

    /**
     * 해결 시간
     */
    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null,

    /**
     * 해결자 (관리자 ID 또는 자동)
     */
    @Column(length = 100, name = "resolved_by")
    var resolvedBy: String? = null,

    /**
     * 해결 방법 메모
     */
    @Column(columnDefinition = "TEXT", name = "resolution_note")
    var resolutionNote: String? = null
) {
    /**
     * DLQ 이벤트를 해결됨으로 표시
     */
    fun markAsResolved(resolvedBy: String, note: String? = null) {
        this.resolved = true
        this.resolvedAt = Instant.now()
        this.resolvedBy = resolvedBy
        this.resolutionNote = note
    }

    /**
     * 디버깅용 toString
     */
    override fun toString(): String {
        return "OutboxDeadLetterEntity(id=$id, sagaId='$sagaId', eventType='$eventType', " +
                "failureReason='$failureReason', failureCount=$failureCount, resolved=$resolved)"
    }
}
