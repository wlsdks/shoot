package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.adapter.`in`.socket.dto.MessageSyncInfoDto
import com.stark.shoot.application.port.`in`.message.command.GetPaginationMessageCommand
import kotlinx.coroutines.flow.Flow

interface GetPaginationMessageUseCase {
    fun getChatMessagesFlow(command: GetPaginationMessageCommand): Flow<MessageSyncInfoDto>
}
