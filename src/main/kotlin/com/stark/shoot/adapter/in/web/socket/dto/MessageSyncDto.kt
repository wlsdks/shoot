package com.stark.shoot.adapter.`in`.web.socket.dto

import java.time.Instant

data class SyncRequestDto(
    val roomId: String,
    val userId: String,
    val lastMessageId: String? = null,
    val lastTimestamp: Instant? = null
)

data class SyncResponseDto(
    val roomId: String,
    val userId: String,
    val messages: List<MessageSyncInfo>,
    val timestamp: Instant,
    val count: Int
)

data class MessageSyncInfo(
    val id: String,
    val tempId: String? = null,
    val timestamp: Instant,
    val senderId: String,
    val status: String
)