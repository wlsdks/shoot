package com.stark.shoot.domain.chat.room

import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomType
import java.time.Instant

data class ChatRoom(
    val id: String? = null,                    // 엔티티의 id(Long)를 String으로 변환
    val title: String? = null,
    val type: ChatRoomType,
    val announcement: String? = null,
    val participants: MutableSet<Long>,        // 참여자 목록 (User id)
    val pinnedParticipants: MutableSet<Long> = mutableSetOf(),  // 고정된 참여자 목록
    val lastMessageId: String? = null,           // 메시지 id (String 변환)
    val lastActiveAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null
)