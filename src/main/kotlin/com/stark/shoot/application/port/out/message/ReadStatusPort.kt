package com.stark.shoot.application.port.out.message

import com.stark.shoot.adapter.`in`.web.dto.message.read.ReadStatus
import com.stark.shoot.domain.chat.room.ChatRoomId
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId

interface ReadStatusPort {
    fun save(readStatus: ReadStatus): ReadStatus
    fun findByRoomIdAndUserId(roomId: ChatRoomId, userId: UserId): ReadStatus?
    fun findAllByRoomId(roomId: ChatRoomId): List<ReadStatus>
    fun updateLastReadMessageId(roomId: ChatRoomId, userId: UserId, messageId: MessageId): ReadStatus
    fun incrementUnreadCount(roomId: ChatRoomId, userId: UserId): ReadStatus
    fun resetUnreadCount(roomId: ChatRoomId, userId: UserId): ReadStatus
}