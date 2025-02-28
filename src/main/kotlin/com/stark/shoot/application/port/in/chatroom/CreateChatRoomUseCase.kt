package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom
import org.bson.types.ObjectId

interface CreateChatRoomUseCase {
    fun createDirectChat(userId: ObjectId, friendId: ObjectId): ChatRoom
}