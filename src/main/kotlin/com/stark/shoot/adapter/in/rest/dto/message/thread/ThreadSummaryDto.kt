package com.stark.shoot.adapter.`in`.rest.dto.message.thread

import com.stark.shoot.adapter.`in`.rest.dto.message.MessageResponseDto
import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class ThreadSummaryDto(
    val rootMessage: MessageResponseDto,
    val replyCount: Long
)
