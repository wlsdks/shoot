package com.stark.shoot.adapter.`in`.rest.dto.message.thread

import com.stark.shoot.adapter.`in`.rest.dto.message.MessageResponseDto
import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class ThreadDetailDto(
    val rootMessage: MessageResponseDto,
    val messages: List<MessageResponseDto>
)
