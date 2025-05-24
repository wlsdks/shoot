package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

data class FriendAddedEvent(
    val userId: Long,
    val friendId: Long
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
