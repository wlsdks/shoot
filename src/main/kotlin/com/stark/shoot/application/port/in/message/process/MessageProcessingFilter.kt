package com.stark.shoot.application.port.`in`.message.process

import com.stark.shoot.domain.chat.message.ChatMessage

interface MessageProcessingFilter {
    suspend fun process(message: ChatMessage, chain: MessageProcessingChain): ChatMessage
}