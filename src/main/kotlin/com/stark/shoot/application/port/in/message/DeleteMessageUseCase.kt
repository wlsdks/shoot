package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.application.port.`in`.message.command.DeleteMessageCommand
import com.stark.shoot.domain.chat.message.ChatMessage

interface DeleteMessageUseCase {
    fun deleteMessage(command: DeleteMessageCommand): ChatMessage
}
