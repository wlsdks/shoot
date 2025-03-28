package com.stark.shoot.application.port.`in`.message.process

import com.stark.shoot.domain.chat.message.ChatMessage

interface MessageProcessingChain {
    suspend fun proceed(message: ChatMessage): ChatMessage
}