package com.stark.shoot.domain.chat.room

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ChatRoomType
import java.time.Instant

data class ChatRoom(
    val id: String? = null,
    val participants: Set<String>,
    val lastMessageId: String? = null,
    val metadata: ChatRoomMetadata,
    val lastActiveAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null
) {
    init {
        require(participants.isNotEmpty()) { "채팅방은 최소 1명 이상의 참여자가 필요합니다." }
        if (metadata.type == ChatRoomType.INDIVIDUAL) {
            require(participants.size == 2) { "1:1 채팅방은 정확히 2명의 참여자가 필요합니다." }
        }
    }
}