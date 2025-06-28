package com.stark.shoot.application.port.`in`.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.application.port.`in`.message.thread.command.GetThreadMessagesCommand

interface GetThreadMessagesUseCase {
    fun getThreadMessages(command: GetThreadMessagesCommand): List<MessageResponseDto>
}
