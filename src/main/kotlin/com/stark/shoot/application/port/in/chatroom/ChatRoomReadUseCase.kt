package com.stark.shoot.application.port.`in`.chatroom

interface ChatRoomReadUseCase {
    fun markAllAsRead(roomId: String, userId: String)
}
