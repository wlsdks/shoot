package com.stark.shoot.application.port.`in`.message.mark

interface MarkMessageReadUseCase {
    fun markMessageAsRead(messageId: String, userId: Long)
    fun markAllMessagesAsRead(roomId: Long, userId: Long, requestId: Long?)
}