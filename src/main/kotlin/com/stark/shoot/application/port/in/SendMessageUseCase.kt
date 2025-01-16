package com.stark.shoot.application.port.`in`

import com.stark.shoot.domain.chat.message.ChatMessage

interface SendMessageUseCase {
    fun sendMessage(roomId: String, senderId: String, messageContent: ChatMessage): ChatMessage
}