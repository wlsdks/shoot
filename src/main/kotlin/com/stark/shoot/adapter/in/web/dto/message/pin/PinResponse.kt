package com.stark.shoot.adapter.`in`.web.dto.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage
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
        fun from(message: ChatMessage): PinResponse {
            return PinResponse(
                messageId = message.id ?: "",
                roomId = message.roomId,
                isPinned = message.isPinned,
                pinnedBy = message.pinnedBy,
                pinnedAt = message.pinnedAt?.toString(),
                content = message.content.text,
                updatedAt = message.updatedAt?.toString() ?: ""
            )
        }
    }
}