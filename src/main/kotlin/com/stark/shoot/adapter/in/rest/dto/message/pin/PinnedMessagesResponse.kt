package com.stark.shoot.adapter.`in`.rest.dto.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.pin.MessagePin
import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class PinnedMessagesResponse(
    val roomId: Long,
    val pinnedMessages: List<PinnedMessageItem>
) {
    companion object {
        /**
         * ChatMessage 리스트와 MessagePin 리스트로부터 PinnedMessagesResponse 생성
         *
         * @param roomId 채팅방 ID
         * @param messages 메시지 리스트
         * @param messagePins 메시지 고정 정보 리스트
         */
        fun from(roomId: Long, messages: List<ChatMessage>, messagePins: List<MessagePin>): PinnedMessagesResponse {
            // MessagePin을 messageId로 매핑
            val pinMap = messagePins.associateBy { it.messageId }

            return PinnedMessagesResponse(
                roomId = roomId,
                pinnedMessages = messages.mapNotNull { message ->
                    message.id?.let { messageId ->
                        pinMap[messageId]?.let { pin ->
                            PinnedMessageItem.from(message, pin)
                        }
                    }
                }
            )
        }
    }
}
