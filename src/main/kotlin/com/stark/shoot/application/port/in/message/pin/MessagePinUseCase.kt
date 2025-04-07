package com.stark.shoot.application.port.`in`.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage

interface MessagePinUseCase {
    fun pinMessage(messageId: String, userId: Long): ChatMessage
    fun unpinMessage(messageId: String, userId: Long): ChatMessage
    fun getPinnedMessages(roomId: Long): List<ChatMessage>
}