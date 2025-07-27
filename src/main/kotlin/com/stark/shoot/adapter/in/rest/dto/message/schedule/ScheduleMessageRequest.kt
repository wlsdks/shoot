package com.stark.shoot.adapter.`in`.rest.dto.message.schedule

import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

data class ScheduleMessageRequest(
    val roomId: Long,
    val content: String,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val scheduledAt: LocalDateTime,
) {
}