package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.shared.UserId
import java.time.Instant

/**
 * 친구 요청 거절 도메인 이벤트
 *
 * @property version Event schema version for MSA compatibility
 */
data class FriendRequestRejectedEvent(
    override val version: EventVersion = EventVersion.FRIEND_REQUEST_REJECTED_V1,
    val senderId: UserId,
    val receiverId: UserId,
    val rejectedAt: Instant,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * FriendRequestRejectedEvent 생성 팩토리 메서드
         *
         * @param senderId 요청을 보낸 사용자 ID (거절당한 사람)
         * @param receiverId 요청을 받은 사용자 ID (거절한 사람)
         * @param rejectedAt 거절 시간
         * @return 생성된 FriendRequestRejectedEvent 객체
         */
        fun create(
            senderId: UserId,
            receiverId: UserId,
            rejectedAt: Instant = Instant.now()
        ): FriendRequestRejectedEvent {
            return FriendRequestRejectedEvent(
                senderId = senderId,
                receiverId = receiverId,
                rejectedAt = rejectedAt
            )
        }
    }
}
