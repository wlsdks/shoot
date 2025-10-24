package com.stark.shoot.adapter.out.persistence.postgres.entity

import com.stark.shoot.domain.saga.SagaState
import jakarta.persistence.*
import java.time.Instant

/**
 * Outbox Pattern을 위한 이벤트 저장 엔티티
 *
 * PostgreSQL 트랜잭션 내에서 이벤트를 저장하여
 * 이벤트 발행을 트랜잭션의 일부로 만듭니다.
 * 별도의 프로세서가 주기적으로 처리합니다.
 */
@Entity
@Table(
    name = "outbox_events",
    indexes = [
        Index(name = "idx_outbox_processed", columnList = "processed"),
        Index(name = "idx_outbox_created_at", columnList = "created_at"),
        Index(name = "idx_outbox_saga_id", columnList = "saga_id"),
        Index(name = "idx_outbox_idempotency_key", columnList = "idempotency_key", unique = true)
    ]
)
class OutboxEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * Saga ID (관련 이벤트들을 그룹핑)
     */
    @Column(nullable = false, length = 100)
    val sagaId: String,

    /**
     * 멱등성 키 (중복 요청 방지)
     * 형식: tempId-senderId 또는 sagaId
     */
    @Column(nullable = false, unique = true, length = 255, name = "idempotency_key")
    val idempotencyKey: String,

    /**
     * 이벤트 타입 (클래스명)
     */
    @Column(nullable = false, length = 255)
    val eventType: String,

    /**
     * 이벤트 페이로드 (JSON)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    /**
     * Saga 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var sagaState: SagaState = SagaState.STARTED,

    /**
     * 처리 완료 여부
     */
    @Column(nullable = false)
    var processed: Boolean = false,

    /**
     * 처리 완료 시간
     */
    @Column(name = "processed_at")
    var processedAt: Instant? = null,

    /**
     * 재시도 횟수
     */
    @Column(nullable = false)
    var retryCount: Int = 0,

    /**
     * 마지막 에러 메시지
     */
    @Column(columnDefinition = "TEXT")
    var lastError: String? = null,

    /**
     * 생성 시간
     */
    @Column(nullable = false, name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),

    /**
     * 수정 시간
     */
    @Column(nullable = false, name = "updated_at")
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 이벤트 처리 완료 처리
     */
    fun markAsProcessed() {
        this.processed = true
        this.processedAt = Instant.now()
        this.updatedAt = Instant.now()
    }

    /**
     * 재시도 증가
     */
    fun incrementRetry(error: String) {
        this.retryCount++
        this.lastError = error
        this.updatedAt = Instant.now()
    }

    /**
     * Saga 상태 업데이트
     */
    fun updateSagaState(newState: SagaState) {
        this.sagaState = newState
        this.updatedAt = Instant.now()
    }
}
