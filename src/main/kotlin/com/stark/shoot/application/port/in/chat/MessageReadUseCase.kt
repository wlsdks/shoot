package com.stark.shoot.application.port.`in`.chat

interface MessageReadUseCase {
    fun markRead(roomId: String, userId: String)
}