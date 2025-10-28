package com.stark.shoot.application.service.event.message

import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.event.MessageDeletedEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 메시지 삭제 이벤트 리스너
 * - 감사 로그 기록
 * - 스레드 메시지 삭제 처리 (부모 메시지 삭제 시 자식 메시지도 삭제)
 * - 향후 삭제 이력 추적, 컴플라이언스 등에 활용
 */
@ApplicationEventListener
class MessageDeletedEventListener(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 삭제 이벤트 처리
     * 트랜잭션 커밋 후 실행되어 데이터 일관성을 보장합니다.
     *
     * 처리 내용:
     * 1. 삭제 로그 기록
     * 2. 스레드 메시지 삭제 (삭제된 메시지가 스레드 루트인 경우)
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

        // 스레드 메시지 삭제 처리
        deleteThreadMessages(event)

        // TODO: 향후 필요 시 삭제 이력 DB 저장 (audit_logs 테이블)
        // TODO: 향후 필요 시 컴플라이언스 시스템 연동
        // TODO: 향후 필요 시 백업 시스템 연동 (복구 가능하도록)
        // TODO: 향후 필요 시 분석 이벤트 전송 (삭제 패턴 분석)
    }

    /**
     * 스레드 메시지 삭제 처리
     * 삭제된 메시지가 스레드 루트인 경우, 해당 스레드의 모든 답글도 삭제합니다.
     *
     * @param event MessageDeletedEvent
     */
    private fun deleteThreadMessages(event: MessageDeletedEvent) {
        try {
            // 삭제된 메시지를 threadId로 가지는 메시지들 조회 (해당 메시지의 답글들)
            val threadMessages = messageQueryPort.findByThreadId(event.messageId)

            if (threadMessages.isEmpty()) {
                logger.debug { "No thread messages found for deleted message: ${event.messageId.value}" }
                return
            }

            logger.info { "Found ${threadMessages.size} thread messages to delete for messageId=${event.messageId.value}" }

            // 모든 스레드 메시지 삭제
            threadMessages.forEach { threadMessage ->
                try {
                    threadMessage.markAsDeleted()
                    messageCommandPort.save(threadMessage)
                    logger.debug { "Deleted thread message: ${threadMessage.id?.value}" }
                } catch (e: Exception) {
                    logger.error(e) {
                        "Failed to delete thread message: ${threadMessage.id?.value}"
                    }
                }
            }

            logger.info { "Successfully deleted ${threadMessages.size} thread messages for messageId=${event.messageId.value}" }

        } catch (e: Exception) {
            logger.error(e) {
                "Error while deleting thread messages for messageId=${event.messageId.value}: ${e.message}"
            }
        }
    }
}
