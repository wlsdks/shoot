package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.EditMessageCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageEditDomainService
import com.stark.shoot.domain.event.MessageEditedEvent
import com.stark.shoot.domain.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 메시지 수정 서비스
 *
 * Slack/Discord 표준 패턴 적용:
 * 1. 메시지를 DB에 영속화 (트랜잭션 내)
 * 2. 트랜잭션 커밋
 * 3. WebSocket 브로드캐스트 (트랜잭션 밖, @TransactionalEventListener에서 처리)
 *
 * 이 패턴의 장점:
 * - 메시지 유실 방지: WebSocket 실패해도 메시지는 DB에 저장됨
 * - 트랜잭션 독립성: 외부 시스템 실패가 트랜잭션을 롤백시키지 않음
 * - 복구 가능성: 클라이언트가 재연결 시 DB에서 메시지 동기화
 */
@Transactional
@UseCase
class EditMessageService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val messageEditDomainService: MessageEditDomainService,
    private val eventPublisher: EventPublishPort
) : EditMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * @apiNote 메시지를 수정합니다.
     * @param command 메시지 수정 커맨드 (메시지 ID, 새로운 내용, 사용자 ID)
     * @throws IllegalArgumentException 메시지를 찾을 수 없거나, 이미 삭제된 메시지이거나, 텍스트 타입이 아닌 경우, 또는 내용이 비어있는 경우
     */
    override fun editMessage(command: EditMessageCommand): ChatMessage {
        // 메시지 조회
        val existingMessage = messageQueryPort.findById(command.messageId)
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=${command.messageId}")

        // 메시지 수정
        val oldContent = existingMessage.content.text
        val updatedMessage = messageEditDomainService.editMessage(existingMessage, command.newContent)

        // DB에 영속화 (트랜잭션 내)
        val savedMessage = messageCommandPort.save(updatedMessage)

        // 도메인 이벤트 발행
        // 트랜잭션 커밋 후 MessageEventWebSocketListener가 WebSocket 전송을 처리
        publishMessageEditedEvent(savedMessage, command.userId, oldContent, savedMessage.content.text)

        logger.info {
            "메시지 수정 완료 (DB 저장): messageId=${savedMessage.id?.value}, " +
            "roomId=${savedMessage.roomId.value}, userId=${command.userId.value}"
        }

        return savedMessage
    }

    /**
     * 메시지 수정 이벤트를 발행합니다.
     *
     * 이벤트는 트랜잭션 커밋 후에 처리되며, MessageEventWebSocketListener가
     * WebSocket 브로드캐스트를 수행합니다.
     */
    private fun publishMessageEditedEvent(
        message: ChatMessage,
        userId: com.stark.shoot.domain.user.vo.UserId,
        oldContent: String,
        newContent: String
    ) {
        try {
            val event = MessageEditedEvent.create(
                messageId = message.id ?: return,
                roomId = message.roomId,
                userId = userId,
                oldContent = oldContent,
                newContent = newContent,
                message = message,  // WebSocket 전송용
                editedAt = Instant.now()
            )
            eventPublisher.publishEvent(event)
            logger.debug { "MessageEditedEvent 발행: messageId=${message.id?.value}" }
        } catch (e: Exception) {
            logger.error(e) { "MessageEditedEvent 발행 실패: messageId=${message.id?.value}" }
        }
    }

}
