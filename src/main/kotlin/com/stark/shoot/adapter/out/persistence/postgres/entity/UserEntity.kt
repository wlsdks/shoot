package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "users")
class UserEntity(
    @Column(unique = true, nullable = false)
    val username: String,  // 로그인용 고유 사용자명

    nickname: String,
    status: UserStatus,

    @Column(unique = true, nullable = false)
    val userCode: String  // 유저 찾기 코드
) : BaseEntity() {

    @Column(nullable = false)
    var nickname: String = nickname
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus = status
        protected set

    var profileImageUrl: String? = null
        protected set

    var lastSeenAt: Instant? = null
        protected set

    var bio: String? = null
        protected set

    var passwordHash: String? = null
        protected set

    @Column(nullable = false)
    var isDeleted: Boolean = false
        protected set

    var refreshToken: String? = null
        protected set

    var refreshTokenExpiration: Instant? = null
        protected set

    // 보조 생성자: 도메인 객체(User)의 모든 필드를 받아서 초기화할 수 있도록 함.
    constructor(
        username: String,
        nickname: String,
        status: UserStatus,
        userCode: String,
        profileImageUrl: String?,
        lastSeenAt: Instant?,
        bio: String?,
        passwordHash: String?,
        isDeleted: Boolean,
        refreshToken: String?,
        refreshTokenExpiration: Instant?
    ) : this(username, nickname, status, userCode) {
        this.profileImageUrl = profileImageUrl
        this.lastSeenAt = lastSeenAt
        this.bio = bio
        this.passwordHash = passwordHash
        this.isDeleted = isDeleted
        this.refreshToken = refreshToken
        this.refreshTokenExpiration = refreshTokenExpiration
    }

}
