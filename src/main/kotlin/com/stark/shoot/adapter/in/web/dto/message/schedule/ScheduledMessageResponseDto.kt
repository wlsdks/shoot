package com.stark.shoot.adapter.`in`.web.dto.message.schedule

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentResponseDto
import com.stark.shoot.infrastructure.enumerate.ScheduledMessageStatus
import java.time.Instant

data class ScheduledMessageResponseDto(
    val id: String,
    val roomId: String,
    val senderId: String,
    val content: MessageContentResponseDto,
    val scheduledAt: Instant,
    val createdAt: Instant,
    val status: ScheduledMessageStatus,
    val metadata: MutableMap<String, Any> = mutableMapOf()
)