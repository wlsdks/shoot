package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom

interface CreateChatRoomUseCase {
    fun createDirectChat(userId: Long, friendId: Long): ChatRoom
}