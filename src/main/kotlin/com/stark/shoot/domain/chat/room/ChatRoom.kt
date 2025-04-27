package com.stark.shoot.domain.chat.room

import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomType
import java.time.Instant

data class ChatRoom(
    val id: Long? = null,
    val title: String? = null,
    val type: ChatRoomType,
    val participants: MutableSet<Long>,
    val lastMessageId: String? = null,
    val lastActiveAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),

    // 필요한 경우에만 남길 선택적 필드
    val announcement: String? = null,
    val pinnedParticipants: MutableSet<Long> = mutableSetOf(),
    val updatedAt: Instant? = null
) {
    /**
     * 채팅방 정보 업데이트
     */
    fun update(
        id: Long? = this.id,
        title: String? = this.title,
        type: ChatRoomType = this.type,
        announcement: String? = this.announcement,
        lastMessageId: String? = this.lastMessageId,
        lastActiveAt: Instant = this.lastActiveAt
    ): ChatRoom {
        return this.copy(
            id = id,
            title = title,
            type = type,
            announcement = announcement,
            lastMessageId = lastMessageId,
            lastActiveAt = lastActiveAt,
            updatedAt = Instant.now()
        )
    }
}
