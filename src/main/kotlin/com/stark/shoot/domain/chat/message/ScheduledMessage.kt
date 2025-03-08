package com.stark.shoot.domain.chat.message

import com.stark.shoot.infrastructure.enumerate.ScheduledMessageStatus
import java.time.Instant

data class ScheduledMessage(
    val id: String? = null,
    val roomId: String,
    val senderId: String,
    val content: MessageContent,
    val scheduledAt: Instant,
    val createdAt: Instant = Instant.now(),
    val status: ScheduledMessageStatus = ScheduledMessageStatus.PENDING,
    val metadata: MutableMap<String, Any> = mutableMapOf()
)
