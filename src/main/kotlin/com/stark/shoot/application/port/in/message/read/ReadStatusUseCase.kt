package com.stark.shoot.application.port.`in`.message.read

interface ReadStatusUseCase {
    fun markMessageAsRead(messageId: String?, userId: Long)
    fun markAllMessagesAsRead(roomId: Long, userId: Long, requestId: String?)
    fun incrementUnreadCount(roomId: Long, userId: Long)
}