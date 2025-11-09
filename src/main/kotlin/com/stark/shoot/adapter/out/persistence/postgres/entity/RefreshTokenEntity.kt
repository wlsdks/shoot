package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false, unique = true, length = 512)
    val token: String,

    @Column(nullable = false)
    val expirationDate: Instant,

    @Column(nullable = true)
    val deviceInfo: String? = null,

    @Column(nullable = true)
    val ipAddress: String? = null,

    @Column(nullable = true)
    var lastUsedAt: Instant? = null,

    @Column(nullable = false)
    var isRevoked: Boolean = false
) : BaseEntity()