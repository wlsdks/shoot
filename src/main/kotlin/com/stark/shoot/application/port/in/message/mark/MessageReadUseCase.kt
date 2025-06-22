package com.stark.shoot.application.port.`in`.message.mark

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

interface MessageReadUseCase {
    fun markMessageAsRead(messageId: MessageId, userId: UserId)
    fun markAllMessagesAsRead(roomId: ChatRoomId, userId: UserId, requestId: String?)
}
