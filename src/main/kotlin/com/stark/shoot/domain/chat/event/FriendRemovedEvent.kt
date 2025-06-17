package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

/**
 * 친구 삭제 도메인 이벤트
 */
data class FriendRemovedEvent(
    val userId: Long,
    val friendId: Long,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * FriendRemovedEvent 생성 팩토리 메서드
         */
        fun create(
            userId: Long,
            friendId: Long
        ): FriendRemovedEvent {
            return FriendRemovedEvent(
                userId = userId,
                friendId = friendId
            )
        }
    }
}
