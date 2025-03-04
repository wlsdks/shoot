package com.stark.shoot.adapter.`in`.web.dto.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage

data class PinnedMessagesResponse(
    val roomId: String,
    val pinnedMessages: List<PinnedMessageItem>
) {
    companion object {
        fun from(roomId: String, messages: List<ChatMessage>): PinnedMessagesResponse {
            return PinnedMessagesResponse(
                roomId = roomId,
                pinnedMessages = messages.map { PinnedMessageItem.from(it) }
            )
        }
    }
}
