package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chat.room.vo.ChatRoomId
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId

interface ReadStatusPort {
    fun updateLastReadMessageId(roomId: ChatRoomId, userId: UserId, messageId: MessageId)
}