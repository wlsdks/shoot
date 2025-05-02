package com.stark.shoot.adapter.`in`.web.dto.message.read

import java.time.Instant

data class ReadStatus(
    val roomId: Long,
    val userId: Long,
    val lastReadMessageId: String?,
    val lastReadAt: Instant,
    val unreadCount: Int
) {
    companion object {
        fun create(
            roomId: Long,
            userId: Long,
            lastReadMessageId: String? = null
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

    fun markAsRead(messageId: String): ReadStatus {
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