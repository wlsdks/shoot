package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.common.DomainEvent

class ChatMessageSentEvent(
    val chatMessage: ChatMessage
) : DomainEvent