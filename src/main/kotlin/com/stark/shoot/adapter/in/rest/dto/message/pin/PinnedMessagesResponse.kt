package com.stark.shoot.adapter.`in`.rest.dto.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class PinnedMessagesResponse(
    val roomId: Long,
    val pinnedMessages: List<PinnedMessageItem>
) {
    companion object {
        fun from(roomId: Long, messages: List<ChatMessage>): PinnedMessagesResponse {
            return PinnedMessagesResponse(
                roomId = roomId,
                pinnedMessages = messages.map { PinnedMessageItem.from(it) }
            )
        }
    }
}
