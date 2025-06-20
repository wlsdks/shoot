package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId

interface ForwardMessageUseCase {
    fun forwardMessage(
        originalMessageId: MessageId,
        targetRoomId: ChatRoomId,
        forwardingUserId: UserId
    ): ChatMessage
}