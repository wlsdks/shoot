package com.stark.shoot.application.service.event.message

import com.stark.shoot.domain.event.MessageDeletedEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 메시지 삭제 이벤트 리스너
 * - 감사 로그 기록
 * - 향후 삭제 이력 추적, 컴플라이언스 등에 활용
 */
@ApplicationEventListener
class MessageDeletedEventListener {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 삭제 이벤트 처리
     * 트랜잭션 커밋 후 실행되어 데이터 일관성을 보장합니다.
     *
     * @param event MessageDeletedEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMessageDeleted(event: MessageDeletedEvent) {
        logger.info {
            "Message deleted: " +
            "messageId=${event.messageId.value}, " +
            "roomId=${event.roomId.value}, " +
            "userId=${event.userId.value}, " +
            "deletedAt=${event.deletedAt}"
        }

        // TODO: 향후 필요 시 삭제 이력 DB 저장 (audit_logs 테이블)
        // TODO: 향후 필요 시 컴플라이언스 시스템 연동
        // TODO: 향후 필요 시 백업 시스템 연동 (복구 가능하도록)
        // TODO: 향후 필요 시 분석 이벤트 전송 (삭제 패턴 분석)
    }
}
