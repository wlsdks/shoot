package com.stark.shoot.domain.event

import com.stark.shoot.domain.shared.UserId
import java.time.Instant

/**
 * 사용자 삭제 도메인 이벤트
 * 사용자 탈퇴/삭제 시 발행되는 이벤트
 */
data class UserDeletedEvent(
    val userId: UserId,
    val username: String,
    val deletedAt: Instant,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * UserDeletedEvent 생성 팩토리 메서드
         *
         * @param userId 삭제된 사용자 ID
         * @param username 사용자 이름
         * @param deletedAt 삭제 시간
         * @return 생성된 UserDeletedEvent 객체
         */
        fun create(
            userId: UserId,
            username: String,
            deletedAt: Instant = Instant.now()
        ): UserDeletedEvent {
            return UserDeletedEvent(
                userId = userId,
                username = username,
                deletedAt = deletedAt
            )
        }
    }
}
