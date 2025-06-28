package com.stark.shoot.application.port.`in`.message.pin

import com.stark.shoot.application.port.`in`.message.pin.command.GetPinnedMessagesCommand
import com.stark.shoot.domain.chat.message.ChatMessage

interface GetPinnedMessageUseCase {
    fun getPinnedMessages(command: GetPinnedMessagesCommand): List<ChatMessage>
}
