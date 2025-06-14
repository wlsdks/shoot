package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse

interface CreateChatRoomUseCase {
    fun createDirectChat(userId: Long, friendId: Long): ChatRoomResponse
}