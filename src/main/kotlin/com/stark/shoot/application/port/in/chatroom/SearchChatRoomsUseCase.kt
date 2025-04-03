package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import org.bson.types.ObjectId

interface SearchChatRoomsUseCase {
    fun searchChatRooms(userId: ObjectId, query: String?, type: String?, unreadOnly: Boolean?): List<ChatRoomResponse>
}