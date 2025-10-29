package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.EditMessageCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageEditDomainService
import com.stark.shoot.domain.event.MessageEditedEvent
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.WebSocketResponseBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@UseCase
class EditMessageService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val messageEditDomainService: MessageEditDomainService,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val eventPublisher: EventPublishPort
) : EditMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * @apiNote 메시지를 수정합니다.
     * @param command 메시지 수정 커맨드 (메시지 ID, 새로운 내용, 사용자 ID)
     * @throws IllegalArgumentException 메시지를 찾을 수 없거나, 이미 삭제된 메시지이거나, 텍스트 타입이 아닌 경우, 또는 내용이 비어있는 경우
     */
    override fun editMessage(command: EditMessageCommand): ChatMessage {
        try {
            // 메시지 조회
            val existingMessage = messageQueryPort.findById(command.messageId)
                ?: run {
                    sendErrorResponse(command.userId, "메시지를 찾을 수 없습니다.")
                    throw IllegalArgumentException("메시지를 찾을 수 없습니다.")
                }

            // 메시지 수정
            val oldContent = existingMessage.content.text
            val updatedMessage = messageEditDomainService.editMessage(existingMessage, command.newContent)
            val savedMessage = messageCommandPort.save(updatedMessage)

            // 도메인 이벤트 발행 (트랜잭션 커밋 후 리스너들이 처리)
            publishMessageEditedEvent(savedMessage, command.userId, oldContent, savedMessage.content.text)

            // 채팅방의 모든 참여자에게 메시지 편집 알림
            webSocketMessageBroker.sendMessage(
                "/topic/message/edit/${savedMessage.roomId.value}",
                savedMessage
            )

            // 요청자에게 성공 응답 전송
            sendSuccessResponse(command.userId, "메시지가 수정되었습니다.", savedMessage)

            return savedMessage

        } catch (e: IllegalArgumentException) {
            sendErrorResponse(command.userId, "메시지 수정에 실패했습니다: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error(e) { "메시지 수정 중 예상치 못한 오류 발생: ${e.message}" }
            sendErrorResponse(command.userId, "메시지 수정 중 오류가 발생했습니다.")
            throw e
        }
    }

    private fun sendSuccessResponse(userId: com.stark.shoot.domain.user.vo.UserId, message: String, data: ChatMessage) {
        webSocketMessageBroker.sendMessage(
            "/queue/message/edit/response/${userId.value}",
            WebSocketResponseBuilder.success(data, message)
        )
    }

    private fun sendErrorResponse(userId: com.stark.shoot.domain.user.vo.UserId, message: String) {
        webSocketMessageBroker.sendMessage(
            "/queue/message/edit/response/${userId.value}",
            WebSocketResponseBuilder.error(message)
        )
    }

    /**
     * 메시지 수정 이벤트를 발행합니다.
     * 트랜잭션 커밋 후 리스너들이 감사 로그, 분석 등의 처리를 수행할 수 있습니다.
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
                editedAt = Instant.now()
            )
            eventPublisher.publishEvent(event)
            logger.debug { "MessageEditedEvent published for message ${message.id?.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish MessageEditedEvent for message ${message.id?.value}" }
        }
    }

}
