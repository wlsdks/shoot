package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import org.bson.types.ObjectId

interface FindChatRoomUseCase {
    fun getChatRoomsForUser(userId: ObjectId): List<ChatRoomResponse>
}