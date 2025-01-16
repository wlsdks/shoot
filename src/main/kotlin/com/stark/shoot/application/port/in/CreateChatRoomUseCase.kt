package com.stark.shoot.application.port.`in`

import com.stark.shoot.domain.chat.room.ChatRoom

interface CreateChatRoomUseCase {
    fun create(title: String?, participants: Set<String>) : ChatRoom
}