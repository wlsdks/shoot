package com.stark.shoot.application.port.`in`.message.schedule

import com.stark.shoot.adapter.`in`.web.dto.message.schedule.ScheduledMessageResponseDto
import java.time.Instant

interface ScheduledMessageUseCase {
    fun scheduleMessage(
        roomId: String,
        senderId: String,
        content: String,
        scheduledAt: Instant
    ): ScheduledMessageResponseDto

    fun cancelScheduledMessage(
        scheduledMessageId: String,
        userId: String
    ): ScheduledMessageResponseDto

    fun updateScheduledMessage(
        scheduledMessageId: String,
        userId: String,
        newContent: String,
        newScheduledAt: Instant? = null
    ): ScheduledMessageResponseDto

    fun getScheduledMessagesByUser(
        userId: String,
        roomId: String? = null
    ): List<ScheduledMessageResponseDto>

    fun sendScheduledMessageNow(
        scheduledMessageId: String,
        userId: String
    ): ScheduledMessageResponseDto
}