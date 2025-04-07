package com.stark.shoot.application.port.`in`.message.schedule

import com.stark.shoot.adapter.`in`.web.dto.message.schedule.ScheduledMessageResponseDto
import java.time.Instant

interface ScheduledMessageUseCase {
    fun scheduleMessage(
        roomId: Long,
        senderId: Long,
        content: String,
        scheduledAt: Instant
    ): ScheduledMessageResponseDto

    fun cancelScheduledMessage(
        scheduledMessageId: String,
        userId: Long
    ): ScheduledMessageResponseDto

    fun updateScheduledMessage(
        scheduledMessageId: String,
        userId: Long,
        newContent: String,
        newScheduledAt: Instant? = null
    ): ScheduledMessageResponseDto

    fun getScheduledMessagesByUser(
        userId: Long,
        roomId: Long? = null
    ): List<ScheduledMessageResponseDto>

    fun sendScheduledMessageNow(
        scheduledMessageId: String,
        userId: Long
    ): ScheduledMessageResponseDto
}