package com.stark.shoot.adapter.`in`.web.socket.dto

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.infrastructure.enumerate.SyncDirection
import java.time.Instant

data class SyncRequestDto(
    val roomId: String,
    val userId: String,
    val lastMessageId: String? = null,
    val lastTimestamp: Instant? = null,
    val direction: SyncDirection = SyncDirection.INITIAL // 기본값은 INITIAL
)

data class SyncResponseDto(
    val roomId: String,
    val userId: String,
    val messages: List<MessageSyncInfoDto>,
    val timestamp: Instant,
    val count: Int,
    val direction: SyncDirection = SyncDirection.INITIAL
)

data class MessageSyncInfoDto(
    val id: String,
    val tempId: String? = null,
    val timestamp: Instant,
    val senderId: String,
    val status: String,
    val content: MessageContentRequest? = null,  // 추가
    val readBy: Map<String, Boolean>? = null  // 추가
)