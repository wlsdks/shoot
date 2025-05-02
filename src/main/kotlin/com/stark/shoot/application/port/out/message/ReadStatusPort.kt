package com.stark.shoot.application.port.out.message

import com.stark.shoot.adapter.`in`.web.dto.message.read.ReadStatus

interface ReadStatusPort {
    fun save(readStatus: ReadStatus): ReadStatus
    fun findByRoomIdAndUserId(roomId: Long, userId: Long): ReadStatus?
    fun findAllByRoomId(roomId: Long): List<ReadStatus>
    fun updateLastReadMessageId(roomId: Long, userId: Long, messageId: String): ReadStatus
    fun incrementUnreadCount(roomId: Long, userId: Long): ReadStatus
    fun resetUnreadCount(roomId: Long, userId: Long): ReadStatus
}