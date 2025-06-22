package com.stark.shoot.domain.user

import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.FriendRequestId
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

/**
 * 친구 요청 애그리게이트
 */
data class FriendRequest(
    val id: FriendRequestId? = null,
    val senderId: UserId,
    val receiverId: UserId,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val respondedAt: Instant? = null,
) {
    companion object {
        /**
         * 친구 요청 생성
         *
         * @param senderId 요청을 보낸 사용자 ID
         * @param receiverId 요청을 받은 사용자 ID
         * @return 생성된 친구 요청
         */
        fun create(senderId: UserId, receiverId: UserId): FriendRequest {
            return FriendRequest(
                senderId = senderId,
                receiverId = receiverId
            )
        }
    }
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
