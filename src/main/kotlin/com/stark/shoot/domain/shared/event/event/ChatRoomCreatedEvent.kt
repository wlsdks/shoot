package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * 새 이벤트 정의
 *
 * @property version Event schema version for MSA compatibility
 */
data class ChatRoomCreatedEvent(
    override val version: EventVersion = EventVersion.CHATROOM_CREATED_V1,
    val roomId: ChatRoomId,
    val userId: UserId,
    override val occurredOn: Long = System.currentTimeMillis()
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
            roomId: ChatRoomId,
            userId: UserId
        ): ChatRoomCreatedEvent {
            return ChatRoomCreatedEvent(
                roomId = roomId,
                userId = userId
            )
        }
    }
}
