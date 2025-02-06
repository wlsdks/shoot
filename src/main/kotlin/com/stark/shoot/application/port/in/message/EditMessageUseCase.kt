package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.chat.message.ChatMessage

interface EditMessageUseCase {
    fun editMessage(messageId: String, newContent: String): ChatMessage
}