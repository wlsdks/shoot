package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.common.vo.UserId

interface LoadPinnedRoomsPort {
    fun findByUserId(userId: UserId): List<ChatRoom>
}
