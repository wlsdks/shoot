package com.stark.shoot.adapter.`in`.web.dto.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class ThreadSummaryDto(
    val rootMessage: MessageResponseDto,
    val replyCount: Long
)
