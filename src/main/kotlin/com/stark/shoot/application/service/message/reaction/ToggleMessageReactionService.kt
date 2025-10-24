package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.adapter.`in`.rest.dto.message.reaction.ReactionResponse
import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.reaction.ToggleMessageReactionUseCase
import com.stark.shoot.application.port.`in`.message.reaction.command.ToggleMessageReactionCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.service.MessageReactionService
import com.stark.shoot.domain.chat.message.vo.ReactionToggleResult
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class ToggleMessageReactionService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val eventPublisher: EventPublishPort,
    private val messageReactionService: MessageReactionService
) : ToggleMessageReactionUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지에 리액션을 토글합니다.
     * 같은 리액션을 선택하면 제거하고, 다른 리액션을 선택하면 기존 리액션을 제거하고 새 리액션을 추가합니다.
     *
     * @param command 메시지 리액션 토글 커맨드
     * @return 업데이트된 메시지의 리액션 정보
     */
    override fun toggleReaction(command: ToggleMessageReactionCommand): ReactionResponse {
        try {
            // 리액션 타입 검증
            val type = ReactionType.fromCode(command.reactionType)
                ?: run {
                    sendErrorResponse(command.userId, "지원하지 않는 리액션 타입입니다: ${command.reactionType}")
                    throw InvalidInputException("지원하지 않는 리액션 타입입니다: ${command.reactionType}")
                }

            // 메시지 조회 (없으면 예외 발생)
            val message = messageQueryPort.findById(command.messageId)
                ?: run {
                    sendErrorResponse(command.userId, "메시지를 찾을 수 없습니다.")
                    throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=${command.messageId}")
                }

            // 도메인 객체에 토글 로직 위임
            val result = message.toggleReaction(command.userId, type)
            val updatedMessage = messageCommandPort.save(result.message)  // result.message 사용

            // 채팅방의 모든 참여자에게 반응 변경 알림
            webSocketMessageBroker.sendMessage(
                "/topic/message/reaction/${updatedMessage.roomId.value}",
                mapOf(
                    "messageId" to updatedMessage.id?.value,
                    "reactions" to updatedMessage.reactions,
                    "userId" to command.userId.value,
                    "reactionType" to command.reactionType,
                    "action" to if (result.isAdded) "ADDED" else "REMOVED"
                )
            )

            // 요청자에게 성공 응답 전송
            val reactionResponse = ReactionResponse.from(
                messageId = updatedMessage.id!!.value,
                reactions = updatedMessage.reactions,
                updatedAt = updatedMessage.updatedAt?.toString() ?: ""
            )

            sendSuccessResponse(command.userId, "반응이 업데이트되었습니다.", reactionResponse)

            // 결과에 따라 알림 및 이벤트 처리
            handleNotificationsAndEvents(result)

            return reactionResponse

        } catch (e: Exception) {
            logger.error(e) { "메시지 반응 처리 중 오류 발생: ${e.message}" }
            sendErrorResponse(command.userId, "반응 처리 중 오류가 발생했습니다: ${e.message}")
            throw e
        }
    }

    private fun sendSuccessResponse(userId: UserId, message: String, data: ReactionResponse) {
        webSocketMessageBroker.sendMessage(
            "/queue/message/reaction/response/${userId.value}",
            mapOf(
                "success" to true,
                "message" to message,
                "data" to data
            )
        )
    }

    private fun sendErrorResponse(userId: UserId, message: String) {
        webSocketMessageBroker.sendMessage(
            "/queue/message/reaction/response/${userId.value}",
            mapOf(
                "success" to false,
                "message" to message,
                "data" to null
            )
        )
    }

    /**
     * 토글 결과에 따라 알림 및 이벤트를 처리합니다.
     *
     * @param messageId 메시지 ID
     * @param result 토글 결과
     */
    private fun handleNotificationsAndEvents(result: ReactionToggleResult) {
        // 도메인 서비스를 통해 이벤트 생성 및 발행
        val events = messageReactionService.processReactionToggleResult(result)
        events.forEach { eventPublisher.publishEvent(it) }
    }

}
