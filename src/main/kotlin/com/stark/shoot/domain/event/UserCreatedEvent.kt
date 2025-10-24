package com.stark.shoot.domain.event

import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

/**
 * 사용자 생성 도메인 이벤트
 * 신규 사용자 가입 시 발행되는 이벤트
 */
data class UserCreatedEvent(
    val userId: UserId,
    val username: String,
    val nickname: String,
    val createdAt: Instant,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * UserCreatedEvent 생성 팩토리 메서드
         *
         * @param userId 생성된 사용자 ID
         * @param username 사용자 이름
         * @param nickname 닉네임
         * @param createdAt 생성 시간
         * @return 생성된 UserCreatedEvent 객체
         */
        fun create(
            userId: UserId,
            username: String,
            nickname: String,
            createdAt: Instant = Instant.now()
        ): UserCreatedEvent {
            return UserCreatedEvent(
                userId = userId,
                username = username,
                nickname = nickname,
                createdAt = createdAt
            )
        }
    }
}
