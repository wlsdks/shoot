package com.stark.shoot.application.port.`in`.message.pin

import com.stark.shoot.application.port.`in`.message.pin.command.PinMessageCommand
import com.stark.shoot.application.port.`in`.message.pin.command.UnpinMessageCommand
import com.stark.shoot.domain.chat.message.ChatMessage

interface MessagePinUseCase {
    fun pinMessage(command: PinMessageCommand): ChatMessage
    fun unpinMessage(command: UnpinMessageCommand): ChatMessage
}
