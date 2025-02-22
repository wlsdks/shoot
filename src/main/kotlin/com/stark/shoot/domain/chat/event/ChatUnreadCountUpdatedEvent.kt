package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

/**
 * 채팅방의 각 참여자의 unreadCount 업데이트 이벤트.
 *
 * @param roomId 채팅방 ID
 * @param unreadCounts 각 참여자(문자열 ID)별 읽지 않은 메시지 수
 */
data class ChatUnreadCountUpdatedEvent(
    val roomId: String,
    val unreadCounts: Map<String, Int>,
    val lastMessage: String? = null
) : DomainEvent
