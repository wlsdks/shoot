package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

interface ReadStatusPort {
    fun updateLastReadMessageId(roomId: ChatRoomId, userId: UserId, messageId: MessageId)
}