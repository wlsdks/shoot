package com.stark.shoot.adapter.`in`.web.dto.message.read

import com.stark.shoot.domain.chat.room.ChatRoomId
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId
import java.time.Instant

data class ReadStatus(
    val roomId: ChatRoomId,
    val userId: UserId,
    val lastReadMessageId: MessageId? = null,
    val lastReadAt: Instant,
    val unreadCount: Int
) {
    companion object {
        fun create(
            roomId: ChatRoomId,
            userId: UserId,
            lastReadMessageId: MessageId? = null
        ): ReadStatus {
            return ReadStatus(
                roomId = roomId,
                userId = userId,
                lastReadMessageId = lastReadMessageId,
                lastReadAt = Instant.now(),
                unreadCount = 0
            )
        }
    }

    fun markAsRead(messageId: MessageId): ReadStatus {
        return copy(
            lastReadMessageId = messageId,
            lastReadAt = Instant.now(),
            unreadCount = 0
        )
    }

    fun incrementUnreadCount(): ReadStatus {
        return copy(
            unreadCount = unreadCount + 1,
            lastReadAt = Instant.now()
        )
    }

}