package com.stark.shoot.application.filter.message.chain

import com.stark.shoot.domain.chat.message.ChatMessage

interface MessageProcessingChain {
    suspend fun proceed(message: ChatMessage): ChatMessage
}