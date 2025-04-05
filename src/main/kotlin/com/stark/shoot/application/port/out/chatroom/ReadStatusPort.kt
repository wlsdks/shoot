package com.stark.shoot.application.port.out.chatroom

interface ReadStatusPort {
    fun updateLastReadMessageId(roomId: Long, userId: Long, messageId: String)
}