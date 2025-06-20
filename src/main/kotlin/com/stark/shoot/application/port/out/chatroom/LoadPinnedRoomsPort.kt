package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.user.vo.UserId

interface LoadPinnedRoomsPort {
    fun findByUserId(userId: UserId): List<ChatRoom>
}
