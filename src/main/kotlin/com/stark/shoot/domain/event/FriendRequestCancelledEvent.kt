package com.stark.shoot.domain.event

import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

/**
 * 친구 요청 취소 도메인 이벤트
 */
data class FriendRequestCancelledEvent(
    val senderId: UserId,
    val receiverId: UserId,
    val cancelledAt: Instant,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * FriendRequestCancelledEvent 생성 팩토리 메서드
         *
         * @param senderId 요청을 보낸 사용자 ID
         * @param receiverId 요청을 받은 사용자 ID
         * @param cancelledAt 취소 시간
         * @return 생성된 FriendRequestCancelledEvent 객체
         */
        fun create(
            senderId: UserId,
            receiverId: UserId,
            cancelledAt: Instant = Instant.now()
        ): FriendRequestCancelledEvent {
            return FriendRequestCancelledEvent(
                senderId = senderId,
                receiverId = receiverId,
                cancelledAt = cancelledAt
            )
        }
    }
}
