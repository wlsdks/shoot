package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.room.ChatRoomId
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId

interface ForwardMessageUseCase {
    fun forwardMessage(
        originalMessageId: MessageId,
        targetRoomId: ChatRoomId,
        forwardingUserId: UserId
    ): ChatMessage
}