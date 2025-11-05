package com.stark.shoot.application.service.event.friend

import com.stark.shoot.domain.shared.event.FriendRemovedEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 친구 삭제 이벤트 리스너
 * - 감사 로그 기록 (알림은 전송하지 않음 - UX 고려)
 *
 * Note: 일반적으로 친구 삭제 시 상대방에게 알림을 보내지 않습니다.
 * 이는 친구 관계 정리에 대한 프라이버시를 존중하는 UX 디자인입니다.
 * 대신 감사 로그를 남겨 추후 분석이나 문제 해결에 활용할 수 있습니다.
 */
@ApplicationEventListener
class FriendRemovedEventListener {

    private val logger = KotlinLogging.logger {}

    /**
     * 친구 삭제 이벤트 처리
     * 트랜잭션 커밋 후 실행되어 데이터 일관성을 보장합니다.
     *
     * @param event FriendRemovedEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleFriendRemoved(event: FriendRemovedEvent) {
        logger.info {
            "Friend removed: " +
            "userId=${event.userId.value}, " +
            "friendId=${event.friendId.value}"
        }

        // TODO: 향후 필요 시 분석 이벤트 전송 (예: 친구 삭제 패턴 분석)
        // TODO: 향후 필요 시 외부 시스템 연동 (CRM 등)
    }
}
