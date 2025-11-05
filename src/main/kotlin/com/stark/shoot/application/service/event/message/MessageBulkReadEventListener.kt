package com.stark.shoot.application.service.event.message

import com.stark.shoot.domain.shared.event.MessageBulkReadEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 메시지 일괄 읽음 이벤트 리스너
 * - 읽음 처리 로그 기록
 * - 향후 읽음률 분석, 사용자 참여도 측정 등에 활용
 */
@ApplicationEventListener
class MessageBulkReadEventListener {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 일괄 읽음 이벤트 처리
     * 트랜잭션 커밋 후 실행되어 데이터 일관성을 보장합니다.
     *
     * @param event MessageBulkReadEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMessageBulkRead(event: MessageBulkReadEvent) {
        logger.info {
            "Messages bulk read: " +
            "roomId=${event.roomId.value}, " +
            "userId=${event.userId.value}, " +
            "messageCount=${event.messageIds.size}"
        }

        logger.debug {
            "Bulk read message IDs: ${event.messageIds.map { it.value }}"
        }

        // TODO: 향후 필요 시 읽음률 분석 데이터 수집
        // TODO: 향후 필요 시 사용자 참여도 측정 (읽은 시간, 패턴 등)
        // TODO: 향후 필요 시 WebSocket으로 읽음 표시 실시간 전송
        // TODO: 향후 필요 시 채팅방 통계 업데이트
    }
}
