package com.stark.shoot.adapter.`in`.rest.dto.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.pin.MessagePin
import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class PinnedMessageItem(
    val messageId: String,
    val content: String,
    val senderId: Long,
    val pinnedBy: Long?,
    val pinnedAt: String?,
    val createdAt: String
) {
    companion object {
        /**
         * ChatMessage와 MessagePin Aggregate로부터 PinnedMessageItem 생성
         *
         * @param message 메시지
         * @param pin 메시지 고정 정보
         */
        fun from(message: ChatMessage, pin: MessagePin): PinnedMessageItem {
            return PinnedMessageItem(
                messageId = message.id?.value ?: "",
                content = message.content.text,
                senderId = message.senderId.value,
                pinnedBy = pin.pinnedBy.value,
                pinnedAt = pin.pinnedAt.toString(),
                createdAt = message.createdAt?.toString() ?: ""
            )
        }
    }
}