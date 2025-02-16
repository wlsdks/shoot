package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom

interface LoadUserPinnedRoomsPort {
    fun findByUserId(userId: String): List<ChatRoom>
}
