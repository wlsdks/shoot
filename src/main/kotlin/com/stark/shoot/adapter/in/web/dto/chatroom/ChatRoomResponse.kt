package com.stark.shoot.adapter.`in`.web.dto.chatroom

data class ChatRoomResponse(
    val roomId: String,
    val title: String,
    val lastMessage: String?,
    val unreadMessages: Int   // 읽지 않은 메시지 수
)