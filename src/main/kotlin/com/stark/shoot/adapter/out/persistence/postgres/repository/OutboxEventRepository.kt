package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.OutboxEventEntity
import com.stark.shoot.domain.saga.SagaState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface OutboxEventRepository : JpaRepository<OutboxEventEntity, Long> {

    /**
     * 처리되지 않은 이벤트 조회 (생성 시간 순)
     */
    fun findByProcessedFalseOrderByCreatedAtAsc(): List<OutboxEventEntity>

    /**
     * 특정 Saga ID의 이벤트 조회
     */
    fun findBySagaIdOrderByCreatedAtAsc(sagaId: String): List<OutboxEventEntity>

    /**
     * 특정 상태의 이벤트 조회
     */
    fun findBySagaStateAndProcessedFalse(sagaState: SagaState): List<OutboxEventEntity>

    /**
     * 오래된 처리 완료 이벤트 삭제용 조회
     * (7일 이상 된 처리 완료 이벤트)
     */
    @Query("""
        SELECT e FROM OutboxEventEntity e
        WHERE e.processed = true
        AND e.processedAt < :threshold
    """)
    fun findOldProcessedEvents(@Param("threshold") threshold: Instant): List<OutboxEventEntity>

    /**
     * 재시도 횟수가 초과된 실패 이벤트 조회
     */
    @Query("""
        SELECT e FROM OutboxEventEntity e
        WHERE e.processed = false
        AND e.retryCount >= :maxRetries
        AND e.sagaState IN ('FAILED', 'COMPENSATING')
    """)
    fun findFailedEventsExceedingRetries(@Param("maxRetries") maxRetries: Int): List<OutboxEventEntity>
}
