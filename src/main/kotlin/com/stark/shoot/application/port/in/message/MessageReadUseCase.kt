package com.stark.shoot.application.port.`in`.message

interface MessageReadUseCase {
    fun markRead(roomId: String, userId: String)
}