package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.application.port.`in`.message.command.SendSyncMessagesToUserCommand

interface SendSyncMessagesToUserUseCase {
    fun sendMessagesToUser(command: SendSyncMessagesToUserCommand)
}