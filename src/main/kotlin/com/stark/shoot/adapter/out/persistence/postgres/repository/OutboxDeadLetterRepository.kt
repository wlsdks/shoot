package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.OutboxDeadLetterEntity
import com.stark.shoot.domain.saga.SagaState
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

/**
 * Outbox Dead Letter Queue Repository
 *
 * 실패한 Outbox 이벤트를 관리하는 Repository입니다.
 *
 * **주요 기능:**
 * 1. 미해결 DLQ 조회 (운영 모니터링용)
 * 2. Saga ID로 DLQ 추적
 * 3. 오래된 해결된 DLQ 정리
 * 4. 통계 조회
 */
interface OutboxDeadLetterRepository : JpaRepository<OutboxDeadLetterEntity, Long> {

    /**
     * 미해결 DLQ 전체 조회 (최신순)
     */
    fun findByResolvedFalseOrderByCreatedAtDesc(): List<OutboxDeadLetterEntity>

    /**
     * 미해결 DLQ 페이징 조회
     */
    fun findByResolvedFalse(pageable: Pageable): Page<OutboxDeadLetterEntity>

    /**
     * 특정 Saga ID의 DLQ 조회
     */
    fun findBySagaIdOrderByCreatedAtDesc(sagaId: String): List<OutboxDeadLetterEntity>

    /**
     * 특정 이벤트 타입의 미해결 DLQ 조회
     */
    fun findByEventTypeAndResolvedFalse(eventType: String): List<OutboxDeadLetterEntity>

    /**
     * 특정 Saga 상태의 미해결 DLQ 조회
     */
    fun findBySagaStateAndResolvedFalse(sagaState: SagaState): List<OutboxDeadLetterEntity>

    /**
     * 미해결 DLQ 개수
     */
    fun countByResolvedFalse(): Long

    /**
     * 특정 기간 내 생성된 DLQ 개수
     */
    @Query("""
        SELECT COUNT(d) FROM OutboxDeadLetterEntity d
        WHERE d.createdAt >= :since
    """)
    fun countDLQSince(@Param("since") since: Instant): Long

    /**
     * 오래된 해결된 DLQ 조회 (정리용)
     * 30일 이상 지난 해결된 DLQ
     */
    @Query("""
        SELECT d FROM OutboxDeadLetterEntity d
        WHERE d.resolved = true
        AND d.resolvedAt < :threshold
    """)
    fun findOldResolvedDLQ(@Param("threshold") threshold: Instant): List<OutboxDeadLetterEntity>

    /**
     * 이벤트 타입별 실패 통계
     */
    @Query("""
        SELECT d.eventType as eventType, COUNT(d) as count
        FROM OutboxDeadLetterEntity d
        WHERE d.resolved = false
        GROUP BY d.eventType
        ORDER BY count DESC
    """)
    fun getFailureStatsByEventType(): List<Map<String, Any>>

    /**
     * 최근 N개 DLQ 조회
     */
    fun findTop10ByResolvedFalseOrderByCreatedAtDesc(): List<OutboxDeadLetterEntity>

    /**
     * 특정 원본 이벤트 ID의 DLQ 존재 여부
     */
    fun existsByOriginalEventId(originalEventId: Long): Boolean
}
