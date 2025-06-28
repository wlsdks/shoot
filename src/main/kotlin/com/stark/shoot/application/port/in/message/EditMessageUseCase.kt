package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.application.port.`in`.message.command.EditMessageCommand
import com.stark.shoot.domain.chat.message.ChatMessage

interface EditMessageUseCase {
    fun editMessage(command: EditMessageCommand): ChatMessage
}
