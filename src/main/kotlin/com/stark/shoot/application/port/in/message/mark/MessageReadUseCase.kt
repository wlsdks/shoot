package com.stark.shoot.application.port.`in`.message.mark

interface MessageReadUseCase {
    fun markMessageAsRead(messageId: String, userId: Long)
    fun markAllMessagesAsRead(roomId: Long, userId: Long, requestId: String?)
}
