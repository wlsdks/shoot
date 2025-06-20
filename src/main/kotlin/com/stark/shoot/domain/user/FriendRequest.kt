package com.stark.shoot.domain.user

import java.time.Instant
import com.stark.shoot.domain.user.type.FriendRequestStatus

/**
 * 친구 요청 애그리게이트
 */
data class FriendRequest(
    val id: Long? = null,
    val senderId: Long,
    val receiverId: Long,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val respondedAt: Instant? = null,
) {
    /**
     * 친구 요청을 수락합니다.
     */
    fun accept(): FriendRequest {
        return copy(status = FriendRequestStatus.ACCEPTED, respondedAt = Instant.now())
    }

    /**
     * 친구 요청을 거절합니다.
     */
    fun reject(): FriendRequest {
        return copy(status = FriendRequestStatus.REJECTED, respondedAt = Instant.now())
    }

    /**
     * 친구 요청을 취소합니다.
     */
    fun cancel(): FriendRequest {
        return copy(status = FriendRequestStatus.CANCELLED, respondedAt = Instant.now())
    }
}
