package com.stark.shoot.adapter.`in`.web.socket.dto

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.infrastructure.enumerate.SyncDirection
import java.time.Instant

data class SyncRequestDto(
    val roomId: Long,
    val userId: Long,
    val lastMessageId: String? = null,
    val lastTimestamp: Instant? = null,
    val direction: SyncDirection = SyncDirection.INITIAL // 기본값은 INITIAL
)

data class SyncResponseDto(
    val roomId: Long,
    val userId: Long,
    val messages: List<MessageSyncInfoDto>,
    val timestamp: Instant,
    val count: Int,
    val direction: SyncDirection = SyncDirection.INITIAL
)

data class MessageSyncInfoDto(
    val id: String,
    val tempId: String? = null,
    val timestamp: Instant,
    val senderId: Long,
    val status: String,
    val content: MessageContentRequest? = null,  // 추가
    val readBy: Map<Long, Boolean>? = null,  // 추가
    val reactions: List<ReactionDto> = emptyList()  // 리액션 추가
)

data class ReactionDto(
    val reactionType: String,
    val emoji: String,
    val description: String,
    val userIds: List<Long>,
    val count: Int
)
