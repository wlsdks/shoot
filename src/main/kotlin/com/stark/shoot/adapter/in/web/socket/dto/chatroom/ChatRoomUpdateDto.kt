package com.stark.shoot.adapter.`in`.web.socket.dto.chatroom

import java.time.Instant

data class ChatRoomUpdateDto(
    val roomId: Long,
    val unreadCount: Int,
    val lastMessage: String?,
    val timestamp: Instant = Instant.now()
)
