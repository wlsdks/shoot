package com.stark.shoot.domain.chat.room

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ChatRoomType
import java.time.Instant

data class ChatRoom(
    val id: String? = null,                    // 채팅방 ID
    val participants: MutableSet<Long>,    // 참여자 (유저) 목록
    val lastMessageId: String? = null,         // 마지막 메시지 ID
    val lastMessageText: String? = null,       // 마지막 메시지 텍스트
    val metadata: ChatRoomMetadata,            // 채팅방 메타데이터
    val lastActiveAt: Instant = Instant.now(), // 마지막 활동 시간
    val createdAt: Instant = Instant.now(),    // 생성 시간
    val updatedAt: Instant? = null             // 업데이트 시간
) {
    init {
        require(participants.isNotEmpty()) { "채팅방은 최소 1명 이상의 참여자가 필요합니다." }

        if (metadata.type == ChatRoomType.INDIVIDUAL) {
            require(participants.size == 2) { "1:1 채팅방은 정확히 2명의 참여자가 필요합니다." }
        }
    }
}