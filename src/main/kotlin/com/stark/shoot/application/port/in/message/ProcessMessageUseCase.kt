package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.application.port.`in`.message.command.ProcessMessageCommand
import com.stark.shoot.domain.chat.message.ChatMessage

interface ProcessMessageUseCase {
    fun processMessageCreate(command: ProcessMessageCommand): ChatMessage
}
