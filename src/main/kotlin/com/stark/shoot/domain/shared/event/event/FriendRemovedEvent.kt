package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.shared.UserId

/**
 * 친구 삭제 도메인 이벤트
 *
 * @property version Event schema version for MSA compatibility
 */
data class FriendRemovedEvent(
    val version: String = "1.0",
    val userId: UserId,
    val friendId: UserId,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * FriendRemovedEvent 생성 팩토리 메서드
         */
        fun create(
            userId: UserId,
            friendId: UserId
        ): FriendRemovedEvent {
            return FriendRemovedEvent(
                userId = userId,
                friendId = friendId
            )
        }
    }
}
