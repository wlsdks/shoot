package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.application.port.`in`.message.command.SendMessageCommand

interface SendMessageUseCase {
    fun sendMessage(command: SendMessageCommand)
}
