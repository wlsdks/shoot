package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.adapter.`in`.web.socket.dto.MessageSyncInfoDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto

/**
 * Command for sending synchronized messages to a user
 */
data class SendSyncMessagesToUserCommand(
    val request: SyncRequestDto,
    val messages: List<MessageSyncInfoDto>,
) {
    companion object {
        fun of(request: SyncRequestDto, messages: List<MessageSyncInfoDto>): SendSyncMessagesToUserCommand {
            return SendSyncMessagesToUserCommand(
                request = request,
                messages = messages,
            )
        }
    }
}
