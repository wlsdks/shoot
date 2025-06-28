package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.application.port.`in`.message.command.ForwardMessageCommand
import com.stark.shoot.domain.chat.message.ChatMessage

interface ForwardMessageUseCase {
    fun forwardMessage(command: ForwardMessageCommand): ChatMessage
}
