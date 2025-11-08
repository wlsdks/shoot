package com.stark.shoot.application.service.event.friend.request

import com.stark.shoot.domain.shared.event.EventVersion
import com.stark.shoot.domain.shared.event.EventVersionValidator
import com.stark.shoot.domain.shared.event.FriendRequestRejectedEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 친구 요청 거절 이벤트 리스너
 * - 감사 로그 기록 (알림은 전송하지 않음 - UX 고려)
 *
 * Note: 일반적으로 친구 요청 거절 시 요청자에게 알림을 보내지 않습니다.
 * 이는 거절당한 사용자의 감정을 고려한 UX 디자인입니다.
 * 대신 감사 로그를 남겨 추후 분석이나 문제 해결에 활용할 수 있습니다.
 */
@ApplicationEventListener
class FriendRequestRejectedEventListener {

    private val logger = KotlinLogging.logger {}

    /**
     * 친구 요청 거절 이벤트 처리
     * 트랜잭션 커밋 후 실행되어 데이터 일관성을 보장합니다.
     *
     * @param event FriendRequestRejectedEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleFriendRequestRejected(event: FriendRequestRejectedEvent) {
        // Event Version 검증
        EventVersionValidator.checkAndLog(event, EventVersion.FRIEND_REQUEST_REJECTED_V1, "FriendRequestRejectedEventListener")

        logger.info {
            "Friend request rejected: " +
            "sender=${event.senderId.value} (rejected), " +
            "receiver=${event.receiverId.value} (rejector), " +
            "rejectedAt=${event.rejectedAt}"
        }

        // TODO: 향후 필요 시 분석 이벤트 전송 (예: 거절 패턴 분석)
        // TODO: 향후 필요 시 외부 시스템 연동 (CRM 등)
    }
}
