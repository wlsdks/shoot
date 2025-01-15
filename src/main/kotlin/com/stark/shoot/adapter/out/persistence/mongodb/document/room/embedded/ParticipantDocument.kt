package com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ParticipantRole
import org.bson.types.ObjectId
import java.time.Instant

data class ParticipantDocument(
    val lastReadMessageId: ObjectId? = null,      // 마지막으로 읽은 메시지 ID
    val lastReadAt: Instant? = null,              // 마지막으로 읽은 시간
    val joinedAt: Instant = Instant.now(),        // 채팅방 참여 시간
    val role: ParticipantRole = ParticipantRole.MEMBER, // 참여자 역할
    val nickname: String? = null,                 // 채팅방 내 별칭 (없으면 null)
    val isActive: Boolean = true                  // 활성 상태 (나가기/초대 상태 표시)
)