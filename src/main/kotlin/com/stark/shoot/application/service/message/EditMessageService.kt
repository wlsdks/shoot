package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.EditMessageCommand
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageEditDomainService
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging

@UseCase
class EditMessageService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val messageEditDomainService: MessageEditDomainService,
    private val webSocketMessageBroker: WebSocketMessageBroker
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
            val updatedMessage = messageEditDomainService.editMessage(existingMessage, command.newContent)
            val savedMessage = messageCommandPort.save(updatedMessage)

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
            mapOf(
                "success" to true,
                "message" to message,
                "data" to data
            )
        )
    }

    private fun sendErrorResponse(userId: com.stark.shoot.domain.user.vo.UserId, message: String) {
        webSocketMessageBroker.sendMessage(
            "/queue/message/edit/response/${userId.value}",
            mapOf(
                "success" to false,
                "message" to message,
                "data" to null
            )
        )
    }

}
