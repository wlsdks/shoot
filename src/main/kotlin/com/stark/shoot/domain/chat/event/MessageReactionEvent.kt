package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

data class MessageReactionEvent(
    val messageId: String,
    val roomId: String,
    val userId: String,
    val reactionType: String,
    val isAdded: Boolean  // true: 추가, false: 제거
) : DomainEvent