package com.stark.shoot.adapter.`in`.web.dto.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage

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
                messageId = message.id ?: "",
                content = message.content.text,
                senderId = message.senderId,
                pinnedBy = message.pinnedBy,
                pinnedAt = message.pinnedAt?.toString(),
                createdAt = message.createdAt?.toString() ?: ""
            )
        }
    }
}