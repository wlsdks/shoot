package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.chat.message.ChatMessage

interface ForwardMessageUseCase {
    fun forwardMessage(
        originalMessageId: String,
        targetRoomId: String,
        forwardingUserId: String
    ): ChatMessage
}