package com.stark.shoot.application.port.`in`.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId

interface MessagePinUseCase {
    fun pinMessage(messageId: MessageId, userId: UserId): ChatMessage
    fun unpinMessage(messageId: MessageId, userId: UserId): ChatMessage
}