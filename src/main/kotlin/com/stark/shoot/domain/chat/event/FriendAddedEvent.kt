package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

data class FriendAddedEvent(
    val userId: Long,
    val friendId: Long
) : DomainEvent