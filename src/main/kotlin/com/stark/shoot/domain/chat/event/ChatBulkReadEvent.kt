package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

data class ChatBulkReadEvent(
    val roomId: String,
    val messageIds: List<String>,
    val userId: String
) : DomainEvent