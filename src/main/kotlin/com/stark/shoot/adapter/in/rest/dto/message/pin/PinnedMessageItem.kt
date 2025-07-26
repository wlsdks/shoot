package com.stark.shoot.adapter.`in`.rest.dto.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage
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
        fun from(message: ChatMessage): PinnedMessageItem {
            return PinnedMessageItem(
                messageId = message.id?.value ?: "",
                content = message.content.text,
                senderId = message.senderId.value,
                pinnedBy = message.pinnedBy?.value,
                pinnedAt = message.pinnedAt?.toString(),
                createdAt = message.createdAt?.toString() ?: ""
            )
        }
    }
}