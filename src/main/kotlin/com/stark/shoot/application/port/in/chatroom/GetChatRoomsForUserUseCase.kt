package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse

interface GetChatRoomsForUserUseCase {
    fun getChatRoomsForUser(userId: Long): List<ChatRoomResponse>
}