package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.*
import java.time.Instant

/**
 * 차단된 사용자 엔티티
 * 한 사용자가 다른 사용자를 차단한 관계를 나타냅니다.
 */
@Entity
@Table(
    name = "blocked_users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_blocked_user_user_id_blocked_id",
            columnNames = ["user_id", "blocked_user_id"]
        )
    ]
)
class BlockedUserEntity(
    // 차단을 수행한 사용자
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    val user: UserEntity,

    // 차단된 사용자
    @ManyToOne(optional = false)
    @JoinColumn(name = "blocked_user_id", referencedColumnName = "id")
    val blockedUser: UserEntity,

    // 차단 시간
    val blockedAt: Instant = Instant.now(),
) : BaseEntity()