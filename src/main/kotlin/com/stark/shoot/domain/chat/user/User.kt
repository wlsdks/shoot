package com.stark.shoot.domain.chat.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.type.UserStatus
import org.bson.types.ObjectId
import java.time.Instant

data class User(
    val id: ObjectId? = null,
    val username: String,
    val nickname: String,
    val status: UserStatus = UserStatus.OFFLINE,
    val profileImageUrl: String? = null,
    val lastSeenAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null,

    // === 친구/소셜 기능 필드 ===
    val friends: Set<ObjectId> = emptySet(),                   // 이미 친구인 사용자들의 ObjectId
    val incomingFriendRequests: Set<ObjectId> = emptySet(),    // 내가 받은(아직 수락/거절 전인) 요청 보낸 사용자 IDs
    val outgoingFriendRequests: Set<ObjectId> = emptySet(),    // 내가 보냈지만 아직 상대방이 수락/거절 안 한 사용자 IDs
)