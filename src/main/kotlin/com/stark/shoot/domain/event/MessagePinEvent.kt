package com.stark.shoot.domain.event

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

data class MessagePinEvent(
    val messageId: MessageId,
    val roomId: ChatRoomId,
    val isPinned: Boolean,
    val userId: UserId,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * 메시지 고정 이벤트 생성
         *
         * @param messageId 메시지 ID
         * @param roomId 채팅방 ID
         * @param isPinned 고정 여부 (true: 고정, false: 고정 해제)
         * @param userId 사용자 ID
         * @return 생성된 MessagePinEvent 객체
         */
        fun create(
            messageId: MessageId,
            roomId: ChatRoomId,
            isPinned: Boolean,
            userId: UserId
        ): MessagePinEvent {
            return MessagePinEvent(
                messageId = messageId,
                roomId = roomId,
                isPinned = isPinned,
                userId = userId
            )
        }
    }
}
