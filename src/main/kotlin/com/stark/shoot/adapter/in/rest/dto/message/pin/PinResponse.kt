package com.stark.shoot.adapter.`in`.rest.dto.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.pin.MessagePin
import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class PinResponse(
    val messageId: String,
    val roomId: Long,
    val isPinned: Boolean,
    val pinnedBy: Long?,
    val pinnedAt: String?,
    val content: String,
    val updatedAt: String
) {
    companion object {
        /**
         * ChatMessage와 MessagePin Aggregate로부터 PinResponse 생성
         *
         * @param message 메시지
         * @param pin 메시지 고정 정보 (null이면 고정되지 않은 메시지)
         */
        fun from(message: ChatMessage, pin: MessagePin?): PinResponse {
            return PinResponse(
                messageId = message.id?.value ?: "",
                roomId = message.roomId.value,
                isPinned = pin != null,
                pinnedBy = pin?.pinnedBy?.value,
                pinnedAt = pin?.pinnedAt?.toString(),
                content = message.content.text,
                updatedAt = message.updatedAt?.toString() ?: ""
            )
        }
    }
}