package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

data class MessagePinEvent(
    val messageId: String,
    val roomId: String,
    val isPinned: Boolean,
    val userId: String
) : DomainEvent