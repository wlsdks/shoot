package com.stark.shoot.domain.social

import com.stark.shoot.domain.social.type.FriendRequestStatus
import com.stark.shoot.domain.social.vo.FriendRequestId
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

/**
 * 친구 요청 애그리게이트
 */
data class FriendRequest(
    val id: FriendRequestId? = null,
    val senderId: UserId,
    val receiverId: UserId,
    var status: FriendRequestStatus = FriendRequestStatus.PENDING,
    var createdAt: Instant = Instant.now(),
    var respondedAt: Instant? = null,
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
     * 자신의 상태를 직접 변경합니다.
     * 
     * @throws IllegalStateException 이미 처리된 요청인 경우
     */
    fun accept() {
        if (status != FriendRequestStatus.PENDING) {
            throw IllegalStateException("이미 처리된 친구 요청입니다: $status")
        }

        status = FriendRequestStatus.ACCEPTED
        respondedAt = Instant.now()
    }

    /**
     * 친구 요청을 거절합니다.
     * 자신의 상태를 직접 변경합니다.
     * 
     * @throws IllegalStateException 이미 처리된 요청인 경우
     */
    fun reject() {
        if (status != FriendRequestStatus.PENDING) {
            throw IllegalStateException("이미 처리된 친구 요청입니다: $status")
        }

        status = FriendRequestStatus.REJECTED
        respondedAt = Instant.now()
    }

    /**
     * 친구 요청을 취소합니다.
     * 자신의 상태를 직접 변경합니다.
     * 
     * @throws IllegalStateException 이미 처리된 요청인 경우
     */
    fun cancel() {
        if (status != FriendRequestStatus.PENDING) {
            throw IllegalStateException("이미 처리된 친구 요청입니다: $status")
        }

        status = FriendRequestStatus.CANCELLED
        respondedAt = Instant.now()
    }

}
