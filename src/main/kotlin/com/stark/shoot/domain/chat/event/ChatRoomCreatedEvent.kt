package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

// 새 이벤트 정의
data class ChatRoomCreatedEvent(
    val roomId: Long,
    val userId: Long
) : DomainEvent