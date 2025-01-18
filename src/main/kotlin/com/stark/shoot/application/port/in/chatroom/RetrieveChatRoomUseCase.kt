package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse

interface RetrieveChatRoomUseCase {
    fun getChatRoomsForUser(userId: String): List<ChatRoomResponse>
}