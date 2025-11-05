package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.shared.UserId
import java.time.Instant

/**
 * 친구 요청 전송 도메인 이벤트
 *
 * @property version Event schema version for MSA compatibility
 */
data class FriendRequestSentEvent(
    val version: String = "1.0",
    val senderId: UserId,
    val receiverId: UserId,
    val sentAt: Instant,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * FriendRequestSentEvent 생성 팩토리 메서드
         *
         * @param senderId 요청을 보낸 사용자 ID
         * @param receiverId 요청을 받은 사용자 ID
         * @param sentAt 요청 전송 시간
         * @return 생성된 FriendRequestSentEvent 객체
         */
        fun create(
            senderId: UserId,
            receiverId: UserId,
            sentAt: Instant = Instant.now()
        ): FriendRequestSentEvent {
            return FriendRequestSentEvent(
                senderId = senderId,
                receiverId = receiverId,
                sentAt = sentAt
            )
        }
    }
}
