package com.stark.shoot.application.port.out.message.readstatus

import com.stark.shoot.adapter.`in`.rest.dto.message.read.ReadStatus
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

interface ReadStatusCommandPort {
    fun save(readStatus: ReadStatus): ReadStatus
    fun updateLastReadMessageId(roomId: ChatRoomId, userId: UserId, messageId: MessageId): ReadStatus
    fun incrementUnreadCount(roomId: ChatRoomId, userId: UserId): ReadStatus
    fun resetUnreadCount(roomId: ChatRoomId, userId: UserId): ReadStatus
}