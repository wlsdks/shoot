package com.stark.shoot.domain.chat.user

import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.UserStatus
import java.time.Instant

data class User(
    val id: Long? = null,
    var username: String,
    var nickname: String,
    var status: UserStatus = UserStatus.OFFLINE,
    var passwordHash: String? = null,
    var userCode: String,
    val createdAt: Instant = Instant.now(),

    // 필요한 경우에만 남길 선택적 필드
    var profileImageUrl: String? = null,
    var lastSeenAt: Instant? = null,
    var bio: String? = null,
    var isDeleted: Boolean = false,
    var updatedAt: Instant? = null,

    // 소셜 기능 관련 필드 (필요시 사용)
    var friendIds: Set<Long> = emptySet(),                 // 이미 친구인 사용자들의 id 목록
    var incomingFriendRequestIds: Set<Long> = emptySet(),  // 받은 친구 요청의 사용자 id 목록
    var outgoingFriendRequestIds: Set<Long> = emptySet(),  // 보낸 친구 요청의 사용자 id 목록
) {
    fun changeUserCode(newCode: String) {
        this.userCode = newCode
    }
}
