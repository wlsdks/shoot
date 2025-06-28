package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.application.port.`in`.message.command.TypingIndicatorCommand

interface TypingIndicatorMessageUseCase {
    fun sendMessage(command: TypingIndicatorCommand)
}
