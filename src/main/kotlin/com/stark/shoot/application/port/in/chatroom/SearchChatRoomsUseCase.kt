package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse

interface SearchChatRoomsUseCase {
    fun searchChatRooms(userId: Long, query: String?, type: String?, unreadOnly: Boolean?): List<ChatRoomResponse>
}