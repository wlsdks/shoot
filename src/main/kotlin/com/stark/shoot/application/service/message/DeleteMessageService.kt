package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.DeleteMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.DeleteMessageCommand
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class DeleteMessageService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val webSocketMessageBroker: WebSocketMessageBroker
) : DeleteMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * @apiNote 메시지 삭제
     * @param command 메시지 삭제 커맨드 (메시지 ID, 사용자 ID)
     */
    override fun deleteMessage(command: DeleteMessageCommand): ChatMessage {
        try {
            // 메시지 로드
            val existingMessage = messageQueryPort.findById(command.messageId)
                ?: run {
                    sendErrorResponse(command.userId, "메시지를 찾을 수 없습니다.")
                    throw IllegalArgumentException("메시지를 찾을 수 없습니다. messageId=${command.messageId}")
                }

            // 도메인 객체의 메서드를 사용하여 메시지 삭제 상태로 변경
            existingMessage.markAsDeleted()
            val savedMessage = messageCommandPort.save(existingMessage)

            // 채팅방의 모든 참여자에게 메시지 삭제 알림
            webSocketMessageBroker.sendMessage(
                "/topic/message/delete/${savedMessage.roomId.value}",
                savedMessage
            )

            // 요청자에게 성공 응답 전송
            sendSuccessResponse(command.userId, "메시지가 삭제되었습니다.", savedMessage)

            return savedMessage

        } catch (e: IllegalArgumentException) {
            sendErrorResponse(command.userId, "메시지 삭제에 실패했습니다: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error(e) { "메시지 삭제 중 예상치 못한 오류 발생: ${e.message}" }
            sendErrorResponse(command.userId, "메시지 삭제 중 오류가 발생했습니다.")
            throw e
        }
    }

    private fun sendSuccessResponse(userId: com.stark.shoot.domain.user.vo.UserId, message: String, data: ChatMessage) {
        webSocketMessageBroker.sendMessage(
            "/queue/message/delete/response/${userId.value}",
            mapOf(
                "success" to true,
                "message" to message,
                "data" to data
            )
        )
    }

    private fun sendErrorResponse(userId: com.stark.shoot.domain.user.vo.UserId, message: String) {
        webSocketMessageBroker.sendMessage(
            "/queue/message/delete/response/${userId.value}",
            mapOf(
                "success" to false,
                "message" to message,
                "data" to null
            )
        )
    }

}
