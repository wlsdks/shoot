package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

// 새 이벤트 정의
data class ChatRoomCreatedEvent(
    val roomId: Long,
    val userId: Long
) : DomainEvent {
    companion object {
        /**
         * 채팅방 생성 이벤트 생성
         *
         * @param roomId 채팅방 ID
         * @param userId 사용자 ID
         * @return 생성된 ChatRoomCreatedEvent 객체
         */
        fun create(
            roomId: Long,
            userId: Long
        ): ChatRoomCreatedEvent {
            return ChatRoomCreatedEvent(
                roomId = roomId,
                userId = userId
            )
        }
    }
}
