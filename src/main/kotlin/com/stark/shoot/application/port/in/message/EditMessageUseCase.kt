package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId

interface EditMessageUseCase {
    fun editMessage(messageId: MessageId, newContent: String): ChatMessage
}