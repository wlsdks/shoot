package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.application.port.`in`.message.reaction.MessageReactionUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.enumerate.ReactionType
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

@UseCase
class MessageReactionService(
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort,
    private val messagingTemplate: SimpMessagingTemplate
) : MessageReactionUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지에 리액션을 추가합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param reactionType 리액션 타입
     * @return 업데이트된 메시지
     */
    override fun addReaction(
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

        val addReactionMessage = processAddReactionMessage(message, type, userId)

        // 저장 및 반환
        val savedMessage = saveMessagePort.save(addReactionMessage)

        // WebSocket으로 실시간 업데이트 전송
        notifyReactionUpdate(messageId, message.roomId, userId, type.code, true)

        // 응답 생성
        return ReactionResponse.from(
            messageId = savedMessage.id ?: messageId,
            reactions = savedMessage.reactions,
            updatedAt = savedMessage.updatedAt?.toString() ?: ""
        )
    }

    /**
     * 메시지에서 리액션을 제거합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param reactionType 리액션 타입
     * @return 업데이트된 메시지
     */
    override fun removeReaction(
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

        // 리액션 제거
        val removedReactionMessage = processRemoveReaction(message, type, userId)

        // 저장 및 반환
        val savedMessage = saveMessagePort.save(removedReactionMessage)

        // WebSocket으로 실시간 업데이트 전송
        notifyReactionUpdate(messageId, message.roomId, userId, type.code, false)

        // 응답 생성
        return ReactionResponse.from(
            messageId = savedMessage.id ?: messageId,
            reactions = savedMessage.reactions,
            updatedAt = savedMessage.updatedAt?.toString() ?: ""
        )
    }

    /**
     * 메시지의 모든 리액션을 가져옵니다.
     *
     * @param messageId 메시지 ID
     * @return 리액션 목록
     */
    override fun getReactions(
        messageId: String
    ): Map<String, Set<Long>> {
        val message = loadMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        return message.reactions
    }

    /**
     * 지원하는 리액션 타입 목록을 가져옵니다.
     *
     * @return 리액션 타입 목록
     */
    override fun getSupportedReactionTypes(): List<ReactionType> {
        return ReactionType.entries
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
        // 특정 채팅방에 있는 모든 클라이언트에게 메시지 반응 업데이트를 전송
        messagingTemplate.convertAndSend(
            "/topic/reactions/$roomId",
            mapOf(
                "messageId" to messageId,
                "userId" to userId,
                "reactionType" to reactionType,
                "emoji" to (ReactionType.fromCode(reactionType)?.emoji ?: "👍"),
                "isAdded" to isAdded
            )
        )
    }

}
