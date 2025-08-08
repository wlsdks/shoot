package com.stark.shoot.adapter.`in`.socket.dto.chatroom

import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class ChatRoomListRequest(
    val userId: Long
)
