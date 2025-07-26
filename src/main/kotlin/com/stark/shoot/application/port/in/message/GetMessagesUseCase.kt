package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.adapter.`in`.rest.dto.message.MessageResponseDto
import com.stark.shoot.application.port.`in`.message.command.GetMessagesCommand

interface GetMessagesUseCase {
    fun getMessages(command: GetMessagesCommand): List<MessageResponseDto>
}
