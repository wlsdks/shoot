package com.stark.shoot.domain.event

import com.stark.shoot.domain.event.DomainEvent

data class FriendAddedEvent(
    val userId: Long,
    val friendId: Long,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * 친구 추가 이벤트 생성
         *
         * @param userId 사용자 ID
         * @param friendId 친구 ID
         * @return 생성된 FriendAddedEvent 객체
         */
        fun create(
            userId: Long,
            friendId: Long
        ): FriendAddedEvent {
            return FriendAddedEvent(
                userId = userId,
                friendId = friendId
            )
        }
    }
}
