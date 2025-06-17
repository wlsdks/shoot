package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

data class MessagePinEvent(
    val messageId: String,
    val roomId: Long,
    val isPinned: Boolean,
    val userId: Long,
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
            messageId: String,
            roomId: Long,
            isPinned: Boolean,
            userId: Long
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
