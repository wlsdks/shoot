package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.chat.message.ChatMessage

interface RetrieveMessageUseCase {
    fun getMessages(roomId: String, lastId: String?, limit: Int): List<ChatMessage>
}