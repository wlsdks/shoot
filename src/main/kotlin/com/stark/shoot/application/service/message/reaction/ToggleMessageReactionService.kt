package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.application.port.`in`.message.reaction.ToggleMessageReactionUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.enumerate.ReactionType
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.springframework.messaging.simp.SimpMessagingTemplate

@UseCase
class ToggleMessageReactionService(
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort,
    private val messagingTemplate: SimpMessagingTemplate
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
        messageId: String,
        userId: Long,
        reactionType: String
    ): ReactionResponse {
        // 리액션 타입 검증
        val type = ReactionType.fromCode(reactionType)
            ?: throw InvalidInputException("지원하지 않는 리액션 타입입니다: $reactionType")

        // 메시지 조회 (없으면 예외 발생)
        val message = loadMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        // 사용자가 이미 추가한 리액션 타입 찾기
        val userExistingReactionType = findUserExistingReactionType(message, userId)

        // 토글 처리
        val updatedMessage = when {
            // 1. 같은 리액션을 선택한 경우: 제거
            userExistingReactionType == type.code ->
                handleRemoveSameReaction(message, messageId, userId, type)

            // 2. 다른 리액션이 이미 있는 경우: 기존 리액션 제거 후 새 리액션 추가
            userExistingReactionType != null ->
                handleReplaceReaction(message, messageId, userId, type, userExistingReactionType)

            // 3. 리액션이 없는 경우: 새 리액션 추가
            else ->
                handleAddNewReaction(message, messageId, userId, type)
        }

        // 저장 및 반환
        val savedMessage = saveMessagePort.save(updatedMessage)

        // 응답 생성
        return ReactionResponse.from(
            messageId = savedMessage.id ?: messageId,
            reactions = savedMessage.reactions,
            updatedAt = savedMessage.updatedAt?.toString() ?: ""
        )
    }

    /**
     * 사용자가 이미 추가한 리액션 타입을 찾습니다.
     *
     * @param message 메시지
     * @param userId 사용자 ID
     * @return 사용자가 추가한 리액션 타입 코드 또는 null
     */
    private fun findUserExistingReactionType(message: ChatMessage, userId: Long): String? {
        return message.reactions.entries // (리액션 타입, 사용자 집합) 쌍을 하나씩 꺼내요.
            .find { (_, users) ->        // 각 쌍에서 users라는 이름으로 Set<Long>을 바인딩
                userId in users          // "users 안에 내가 전달한 userId가 있는지"를 검사합니다.
            }?.key                       // 찾은 쌍이 있으면 그 key(리액션 타입)를, 없으면 null
    }

    /**
     * 같은 리액션을 선택한 경우 처리: 리액션 제거
     *
     * @param message 메시지
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param type 리액션 타입
     * @return 업데이트된 메시지
     */
    private fun handleRemoveSameReaction(
        message: ChatMessage,
        messageId: String,
        userId: Long,
        type: ReactionType
    ): ChatMessage {
        val updatedMessage = processRemoveReaction(message, type, userId)

        // WebSocket으로 실시간 업데이트 전송 (제거)
        notifyReactionUpdate(messageId, message.roomId, userId, type.code, false)

        return updatedMessage
    }

    /**
     * 다른 리액션이 이미 있는 경우 처리: 기존 리액션 제거 후 새 리액션 추가
     *
     * @param message 메시지
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param newType 새 리액션 타입
     * @param existingTypeCode 기존 리액션 타입 코드
     * @return 업데이트된 메시지
     */
    private fun handleReplaceReaction(
        message: ChatMessage,
        messageId: String,
        userId: Long,
        newType: ReactionType,
        existingTypeCode: String
    ): ChatMessage {
        // 기존 리액션 제거
        val existingType = ReactionType.fromCode(existingTypeCode)
            ?: throw InvalidInputException("지원하지 않는 리액션 타입입니다: $existingTypeCode")

        val messageAfterRemove = processRemoveReaction(message, existingType, userId)

        // WebSocket으로 실시간 업데이트 전송 (기존 리액션 제거)
        notifyReactionUpdate(messageId, message.roomId, userId, existingType.code, false)

        // 새 리액션 추가
        val messageAfterAdd = processAddReactionMessage(messageAfterRemove, newType, userId)

        // WebSocket으로 실시간 업데이트 전송 (새 리액션 추가)
        notifyReactionUpdate(messageId, message.roomId, userId, newType.code, true)

        return messageAfterAdd
    }

    /**
     * 리액션이 없는 경우 처리: 새 리액션 추가
     *
     * @param message 메시지
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param type 리액션 타입
     * @return 업데이트된 메시지
     */
    private fun handleAddNewReaction(
        message: ChatMessage,
        messageId: String,
        userId: Long,
        type: ReactionType
    ): ChatMessage {
        val updatedMessage = processAddReactionMessage(message, type, userId)

        // WebSocket으로 실시간 업데이트 전송 (추가)
        notifyReactionUpdate(messageId, message.roomId, userId, type.code, true)

        return updatedMessage
    }

    /**
     * 메시지에 리액션을 추가합니다.
     *
     * @param message 메시지
     * @param type 리액션 타입
     * @param userId 사용자 ID
     * @return 업데이트된 메시지
     */
    private fun processAddReactionMessage(
        message: ChatMessage,
        type: ReactionType,
        userId: Long
    ): ChatMessage {
        // 도메인 객체의 메서드를 사용하여 리액션 추가
        return message.addReaction(userId, type.code)
    }

    /**
     * 메시지에서 리액션을 제거합니다.
     *
     * @param message 메시지
     * @param type 리액션 타입
     * @param userId 사용자 ID
     * @return 업데이트된 메시지
     */
    private fun processRemoveReaction(
        message: ChatMessage,
        type: ReactionType,
        userId: Long
    ): ChatMessage {
        // 도메인 객체의 메서드를 사용하여 리액션 제거
        return message.removeReaction(userId, type.code)
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
        messageId: String,
        roomId: Long,
        userId: Long,
        reactionType: String,
        isAdded: Boolean
    ) {
        val type = ReactionType.fromCode(reactionType) ?: ReactionType.LIKE

        // 특정 채팅방에 있는 모든 클라이언트에게 메시지 반응 업데이트를 전송
        messagingTemplate.convertAndSend(
            "/topic/reactions/$roomId",
            mapOf(
                "messageId" to messageId,
                "userId" to userId,
                "reactionType" to reactionType,
                "emoji" to type.emoji,
                "isAdded" to isAdded
            )
        )
    }

}
