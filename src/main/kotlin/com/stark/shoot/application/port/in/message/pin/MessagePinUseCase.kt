package com.stark.shoot.application.port.`in`.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage

interface MessagePinUseCase {
    fun pinMessage(messageId: String, userId: String): ChatMessage
    fun unpinMessage(messageId: String, userId: String): ChatMessage
    fun getPinnedMessages(roomId: String): List<ChatMessage>
}