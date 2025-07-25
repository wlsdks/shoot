package com.stark.shoot.adapter.`in`.rest.dto.message.schedule

import com.stark.shoot.adapter.`in`.rest.dto.message.MessageContentResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageMetadataResponseDto
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.infrastructure.annotation.ApplicationDto
import java.time.Instant

@ApplicationDto
data class ScheduledMessageResponseDto(
    val id: String,
    val roomId: Long,
    val senderId: Long,
    val content: MessageContentResponseDto,
    val scheduledAt: Instant,
    val createdAt: Instant,
    val status: ScheduledMessageStatus,
    val metadata: MessageMetadataResponseDto
)