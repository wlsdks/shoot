package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.DeleteMessageUseCase
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.application.port.`in`.message.command.DeleteMessageCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.shared.event.MessageDeletedEvent
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

/**
 * 메시지 삭제 서비스
 *
 * Slack/Discord 표준 패턴 적용:
 * 1. 메시지를 DB에 영속화 (MongoDB - 단일 document는 atomic 보장)
 * 2. 저장 완료 확인
 * 3. WebSocket 브로드캐스트 (이벤트 리스너에서 비동기 처리)
 *
 * MongoDB 특성:
 * - 단일 document 작업은 원자적(atomic)으로 처리됨
 * - @Transactional 불필요 (save() 성공 or 실패, 중간 상태 없음)
 *
 * 이 패턴의 장점:
 * - 메시지 유실 방지: WebSocket 실패해도 메시지는 이미 MongoDB에 저장됨
 * - 작업 독립성: 외부 시스템(WebSocket) 실패가 저장 작업에 영향 없음
 * - 복구 가능성: 클라이언트가 재연결 시 MongoDB에서 메시지 동기화 가능
 */
@UseCase
class DeleteMessageService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val eventPublisher: EventPublishPort
) : DeleteMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * @apiNote 메시지 삭제
     * @param command 메시지 삭제 커맨드 (메시지 ID, 사용자 ID)
     */
    override fun deleteMessage(command: DeleteMessageCommand): ChatMessage {
        // 메시지 로드
        val existingMessage = messageQueryPort.findById(command.messageId)
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=${command.messageId}")

        // 도메인 객체의 메서드를 사용하여 메시지 삭제 상태로 변경
        existingMessage.markAsDeleted()

        // MongoDB에 영속화 (atomic 작업)
        val savedMessage = messageCommandPort.save(existingMessage)

        // 도메인 이벤트 발행
        // MessageEventWebSocketListener가 저장 완료 후 WebSocket 전송을 처리
        publishMessageDeletedEvent(savedMessage, command.userId)

        logger.info {
            "메시지 삭제 완료 (MongoDB 저장): messageId=${savedMessage.id?.value}, " +
            "roomId=${savedMessage.roomId.value}, userId=${command.userId.value}"
        }

        return savedMessage
    }

    /**
     * 메시지 삭제 이벤트를 발행합니다.
     *
     * 이벤트 리스너(MessageEventWebSocketListener)가 저장 완료 후
     * WebSocket 브로드캐스트를 수행합니다.
     */
    private fun publishMessageDeletedEvent(
        message: ChatMessage,
        userId: com.stark.shoot.domain.shared.UserId
    ) {
        try {
            val event = MessageDeletedEvent.create(
                messageId = message.id ?: return,
                roomId = message.roomId,
                userId = userId,
                message = message,  // WebSocket 전송용
                deletedAt = Instant.now()
            )
            eventPublisher.publishEvent(event)
            logger.debug { "MessageDeletedEvent 발행: messageId=${message.id?.value}" }
        } catch (e: Exception) {
            logger.error(e) { "MessageDeletedEvent 발행 실패: messageId=${message.id?.value}" }
        }
    }

}
