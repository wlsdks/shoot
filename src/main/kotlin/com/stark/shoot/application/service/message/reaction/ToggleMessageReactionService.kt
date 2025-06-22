package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.application.port.`in`.message.reaction.ToggleMessageReactionUseCase
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.service.MessageReactionService
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.message.vo.ReactionToggleResult
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.messaging.simp.SimpMessagingTemplate

@UseCase
class ToggleMessageReactionService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val messagingTemplate: SimpMessagingTemplate,
    private val eventPublisher: EventPublisher,
    private val messageReactionService: MessageReactionService
) : ToggleMessageReactionUseCase {

    /**
     * 메시지에 리액션을 토글합니다.
     * 같은 리액션을 선택하면 제거하고, 다른 리액션을 선택하면 기존 리액션을 제거하고 새 리액션을 추가합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param reactionType 리액션 타입
     * @return 업데이트된 메시지의 리액션 정보
     */
    override fun toggleReaction(
        messageId: MessageId,
        userId: UserId,
        reactionType: String
    ): ReactionResponse {
        // 리액션 타입 검증
        val type = ReactionType.fromCode(reactionType)
            ?: throw InvalidInputException("지원하지 않는 리액션 타입입니다: $reactionType")

        // 메시지 조회 (없으면 예외 발생)
        val message = messageQueryPort.findById(messageId)
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        // 도메인 객체에 토글 로직 위임
        val result = message.toggleReaction(userId, type)

        // 결과에 따라 알림 및 이벤트 처리
        handleNotificationsAndEvents(messageId, result)

        // 저장 및 반환
        val savedMessage = messageCommandPort.save(result.message)

        // 응답 생성
        // savedMessage.reactions는 ChatMessage의 getter를 통해 messageReactions.reactions에 접근
        return ReactionResponse.from(
            messageId = savedMessage.id!!.value,
            reactions = savedMessage.reactions,
            updatedAt = savedMessage.updatedAt?.toString() ?: ""
        )
    }

    /**
     * 토글 결과에 따라 알림 및 이벤트를 처리합니다.
     *
     * @param messageId 메시지 ID
     * @param result 토글 결과
     */
    private fun handleNotificationsAndEvents(
        messageId: MessageId,
        result: ReactionToggleResult
    ) {
        val message = result.message
        val userId = result.userId // 리액션을 토글한 사용자 ID

        // 리액션 교체인 경우 (기존 리액션 제거 후 새 리액션 추가)
        if (result.isReplacement && result.previousReactionType != null) {
            // 기존 리액션 제거 알림
            notifyReactionUpdate(messageId, message.roomId, userId, result.previousReactionType, false)

            // 새 리액션 추가 알림
            notifyReactionUpdate(messageId, message.roomId, userId, result.reactionType, true)
        } else {
            // 일반 추가/제거 알림
            notifyReactionUpdate(messageId, message.roomId, userId, result.reactionType, result.isAdded)
        }

        // 도메인 서비스를 통해 이벤트 생성 및 발행
        val events = messageReactionService.processReactionToggleResult(result)
        events.forEach { eventPublisher.publish(it) }
    }

    /**
     * 메시지 반응 업데이트를 WebSocket으로 전송합니다.
     *
     * @param messageId 메시지 ID
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param reactionType 리액션 타입
     * @param isAdded 추가 여부
     */
    private fun notifyReactionUpdate(
        messageId: MessageId,
        roomId: ChatRoomId,
        userId: UserId,
        reactionType: String,
        isAdded: Boolean
    ) {
        val type = ReactionType.fromCode(reactionType) ?: ReactionType.LIKE

        // 특정 채팅방에 있는 모든 클라이언트에게 메시지 반응 업데이트를 전송
        messagingTemplate.convertAndSend(
            "/topic/reactions/${roomId.value}",
            mapOf(
                "messageId" to messageId.value,
                "userId" to userId.value,
                "reactionType" to reactionType,
                "emoji" to type.emoji,
                "isAdded" to isAdded
            )
        )
    }

}
