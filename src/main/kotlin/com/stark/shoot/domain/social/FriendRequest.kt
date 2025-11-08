package com.stark.shoot.domain.social

import com.stark.shoot.domain.shared.event.FriendAddedEvent
import com.stark.shoot.domain.social.type.FriendRequestStatus
import com.stark.shoot.domain.social.vo.FriendRequestId
import com.stark.shoot.domain.shared.UserId
import java.time.Instant

/**
 * 친구 요청 애그리게이트
 *
 * DDD Rich Model: 친구 요청 수락 시 Friendship과 Event를 직접 생성합니다.
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
     *
     * DDD Rich Model 개선:
     * - 상태 변경뿐만 아니라 Friendship과 Event도 직접 생성
     * - 비즈니스 로직이 도메인 모델 내부에 위치
     * - FriendDomainService의 역할 최소화
     *
     * @return FriendshipPair (양방향 Friendship + 이벤트)
     * @throws IllegalStateException 이미 처리된 요청인 경우
     */
    fun accept(): FriendshipPair {
        if (status != FriendRequestStatus.PENDING) {
            throw IllegalStateException("이미 처리된 친구 요청입니다: $status")
        }

        status = FriendRequestStatus.ACCEPTED
        respondedAt = Instant.now()

        // 비즈니스 로직 내재화: Friendship 생성
        val friendship1 = Friendship.create(
            userId = receiverId,
            friendId = senderId
        )
        val friendship2 = Friendship.create(
            userId = senderId,
            friendId = receiverId
        )

        // 비즈니스 로직 내재화: Event 생성
        val events = listOf(
            FriendAddedEvent.create(
                userId = receiverId,
                friendId = senderId
            ),
            FriendAddedEvent.create(
                userId = senderId,
                friendId = receiverId
            )
        )

        return FriendshipPair(
            friendship1 = friendship1,
            friendship2 = friendship2,
            events = events
        )
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
