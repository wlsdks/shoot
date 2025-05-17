package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

data class MessageReactionEvent(
    val messageId: String,
    val roomId: String,
    val userId: String,
    val reactionType: String,
    val isAdded: Boolean,  // true: 추가, false: 제거
    val isReplacement: Boolean = false  // true: 리액션 교체의 일부, false: 일반 추가/제거
) : DomainEvent
