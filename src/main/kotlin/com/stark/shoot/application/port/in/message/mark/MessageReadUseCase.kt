package com.stark.shoot.application.port.`in`.message.mark

import com.stark.shoot.domain.chat.room.vo.ChatRoomId
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId

interface MessageReadUseCase {
    fun markMessageAsRead(messageId: MessageId, userId: UserId)
    fun markAllMessagesAsRead(roomId: ChatRoomId, userId: UserId, requestId: String?)
}
