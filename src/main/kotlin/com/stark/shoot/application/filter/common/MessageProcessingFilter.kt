package com.stark.shoot.application.filter.common

import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.domain.chat.message.ChatMessage

interface MessageProcessingFilter {
    suspend fun process(message: ChatMessage, chain: MessageProcessingChain): ChatMessage
}