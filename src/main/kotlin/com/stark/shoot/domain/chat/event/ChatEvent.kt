package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.chat.message.ChatMessage
import java.time.Instant

data class ChatEvent(
    val type: EventType,
    val data: ChatMessage,
    val timestamp: Instant = Instant.now()
)