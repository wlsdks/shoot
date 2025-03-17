package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.chat.message.ChatMessage

interface ProcessMessageUseCase {
    suspend fun processMessageCreate(message: ChatMessage): ChatMessage
}