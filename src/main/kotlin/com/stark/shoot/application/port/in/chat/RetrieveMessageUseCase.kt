package com.stark.shoot.application.port.`in`.chat

import com.stark.shoot.domain.chat.message.ChatMessage
import java.time.Instant

interface RetrieveMessageUseCase {
    fun getMessages(roomId: String, before: Instant?, limit: Int): List<ChatMessage>
}