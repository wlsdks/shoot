package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.application.port.`in`.message.reaction.MessageReactionUseCase
import com.stark.shoot.application.port.out.message.LoadChatMessagePort
import com.stark.shoot.application.port.out.message.SaveChatMessagePort
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
    private val loadChatMessagePort: LoadChatMessagePort,
    private val saveChatMessagePort: SaveChatMessagePort,
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
        userId: String,
        reactionType: String
    ): ReactionResponse {
        // 리액션 타입 검증
        val type = ReactionType.fromCode(reactionType)
            ?: throw InvalidInputException("지원하지 않는 리액션 타입입니다: $reactionType")

        // 메시지 조회 (없으면 예외 발생)
        val message = loadChatMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        val addReactionMessage = processAddReactionMessage(message, type, userId)

        // 저장 및 반환
        val savedMessage = saveChatMessagePort.save(addReactionMessage)

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
        userId: String,
        reactionType: String
    ): ReactionResponse {
        // 리액션 타입 검증
        val type = ReactionType.fromCode(reactionType)
            ?: throw InvalidInputException("지원하지 않는 리액션 타입입니다: $reactionType")

        // 메시지 조회 (없으면 예외 발생)
        val message = loadChatMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        // 리액션 제거
        val removedReactionMessage = processRemoveReaction(message, type, userId)

        // 저장 및 반환
        val savedMessage = saveChatMessagePort.save(removedReactionMessage)

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
    ): Map<String, Set<String>> {
        val message = loadChatMessagePort.findById(messageId.toObjectId())
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
        userId: String
    ): ChatMessage {
        // 기존 반응 맵을 복사
        val updatedReactions = message.reactions.toMutableMap()

        // 해당 반응 타입에 대한 사용자 목록 가져오기 또는 새로 생성
        val usersForReaction = updatedReactions.getOrDefault(type.code, emptySet()).toMutableSet()

        // 사용자 추가
        usersForReaction.add(userId)

        // 업데이트된 사용자 목록을 맵에 설정
        updatedReactions[type.code] = usersForReaction

        // 메시지 복사본 만들기 (불변성 유지)
        val updatedMessage = message.copy(
            reactions = updatedReactions,
            updatedAt = Instant.now()
        )

        return updatedMessage
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
        userId: String
    ): ChatMessage {
        // 기존 반응을 수정할 수 있도록 복사
        val updatedReactions = message.reactions.toMutableMap()

        // 해당 반응 타입에 대한 사용자 목록이 있으면 제거
        if (updatedReactions.containsKey(type.code)) {
            val usersForReaction = updatedReactions[type.code]!!.toMutableSet()
            usersForReaction.remove(userId)

            if (usersForReaction.isEmpty()) {
                updatedReactions.remove(type.code)
            } else {
                updatedReactions[type.code] = usersForReaction
            }
        }

        // 메시지 복사본 만들기 (불변성 유지)
        val updatedMessage = message.copy(
            reactions = updatedReactions,
            updatedAt = Instant.now()
        )

        return updatedMessage
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
        roomId: String,
        userId: String,
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