package com.stark.shoot.application.port.`in`.message.mark

import com.stark.shoot.application.port.`in`.message.mark.command.MarkAllMessagesAsReadCommand
import com.stark.shoot.application.port.`in`.message.mark.command.MarkMessageAsReadCommand

interface MessageReadUseCase {
    fun markMessageAsRead(command: MarkMessageAsReadCommand)
    fun markAllMessagesAsRead(command: MarkAllMessagesAsReadCommand)
}
