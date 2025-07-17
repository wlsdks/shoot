package com.stark.shoot.domain.event

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

/**
 * 채팅방 업데이트 이벤트.
 * 각 사용자별 읽지 않은 메시지 수와 마지막 메시지 정보를 전달합니다.
 */
data class ChatRoomUpdateEvent(
    val roomId: ChatRoomId,
    val updates: Map<UserId, Update>,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    data class Update(
        val unreadCount: Int,
        val lastMessage: String?
    )

    companion object {
        /**
         * 채팅방 업데이트 이벤트 생성
         */
        fun create(
            roomId: ChatRoomId,
            updates: Map<UserId, Update>
        ): ChatRoomUpdateEvent {
            return ChatRoomUpdateEvent(
                roomId = roomId,
                updates = updates
            )
        }
    }
}
