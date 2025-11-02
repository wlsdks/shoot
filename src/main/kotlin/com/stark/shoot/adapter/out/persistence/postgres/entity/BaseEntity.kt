package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0

    @CreatedDate
    @Column(nullable = false, updatable = false)
    open val createdAt: Instant = Instant.now()

    @LastModifiedDate
    open var updatedAt: Instant? = null

    /**
     * 낙관적 락(Optimistic Locking)을 위한 버전 필드
     *
     * JPA가 자동으로 관리:
     * - 엔티티 수정 시마다 version 자동 증가
     * - 동시 수정 시도 시 OptimisticLockException 발생
     *
     * Race Condition 방지:
     * - 친구 요청 동시 취소/수락
     * - RefreshToken 동시 갱신
     * - 기타 동시성 문제 방지
     */
    @Version
    @Column(nullable = false)
    open var version: Long = 0

    override fun equals(other: Any?): Boolean =
        this === other || (other is BaseEntity && id == other.id)

    override fun hashCode(): Int = id.hashCode()

}