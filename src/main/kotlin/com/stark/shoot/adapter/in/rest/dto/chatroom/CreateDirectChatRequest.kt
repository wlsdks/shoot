package com.stark.shoot.adapter.`in`.rest.dto.chatroom

data class CreateDirectChatRequest(
    val userId: Long,
    val friendId: Long
) {}