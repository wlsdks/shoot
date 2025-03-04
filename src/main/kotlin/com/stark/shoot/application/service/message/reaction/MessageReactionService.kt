package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.application.port.`in`.message.reaction.MessageReactionUseCase
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.LoadChatMessagePort
import com.stark.shoot.application.port.out.message.SaveChatMessagePort
import com.stark.shoot.domain.chat.event.MessageReactionEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.common.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class MessageReactionService(
    private val loadChatMessagePort: LoadChatMessagePort,
    private val saveChatMessagePort: SaveChatMessagePort,
    private val eventPublisher: EventPublisher,
    private val messagingTemplate: SimpMessagingTemplate
) : MessageReactionUseCase {

    /**
     * 메시지에 반응 추가
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param reactionType 반응 타입
     * @return 업데이트된 메시지
     */
    override fun addReaction(
        messageId: String,
        userId: String,
        reactionType: String
    ): ChatMessage {
        val message = loadChatMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        // 기존 반응 맵을 복사
        val updatedReactions = message.reactions.toMutableMap()

        // 해당 반응 타입에 대한 사용자 목록 가져오기 또는 새로 생성
        val usersForReaction = updatedReactions.getOrDefault(reactionType, emptySet()).toMutableSet()

        // 사용자 추가
        usersForReaction.add(userId)

        // 업데이트된 사용자 목록을 맵에 설정
        updatedReactions[reactionType] = usersForReaction

        // 메시지 복사본 만들기 (불변성 유지)
        val updatedMessage = message.copy(reactions = updatedReactions)

        // 저장 및 반환
        val savedMessage = saveChatMessagePort.save(updatedMessage)

        // WebSocket으로 실시간 업데이트 전송
        notifyReactionUpdate(messageId, message.roomId, userId, reactionType, true)

        // 이벤트 발행 (필요 시)
        eventPublisher.publish(
            MessageReactionEvent(
                messageId = messageId,
                roomId = message.roomId,
                userId = userId,
                reactionType = reactionType,
                isAdded = true
            )
        )

        return savedMessage
    }


    /**
     * 메시지에서 반응 제거
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param reactionType 반응 타입
     * @return 업데이트된 메시지
     */
    override fun removeReaction(
        messageId: String,
        userId: String,
        reactionType: String
    ): ChatMessage {
        val message = loadChatMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        // 기존 반응을 수정할 수 있도록 복사
        val updatedReactions = message.reactions.toMutableMap()

        // 해당 반응 타입에 대한 사용자 목록이 있으면
        if (updatedReactions.containsKey(reactionType)) {
            val usersForReaction = updatedReactions[reactionType]!!.toMutableSet()

            // 사용자 제거
            usersForReaction.remove(userId)

            // 사용자가 없으면 반응 타입도 제거, 있으면 업데이트
            if (usersForReaction.isEmpty()) {
                updatedReactions.remove(reactionType)
            } else {
                updatedReactions[reactionType] = usersForReaction
            }
        }

        // 메시지 복사본 만들기 (불변성 유지)
        val updatedMessage = message.copy(reactions = updatedReactions)

        // 저장 및 반환
        val savedMessage = saveChatMessagePort.save(updatedMessage)

        // WebSocket으로 실시간 업데이트 전송
        notifyReactionUpdate(messageId, message.roomId, userId, reactionType, false)

        // 이벤트 발행 (필요 시)
        eventPublisher.publish(
            MessageReactionEvent(
                messageId = messageId,
                roomId = message.roomId,
                userId = userId,
                reactionType = reactionType,
                isAdded = false
            )
        )

        return savedMessage
    }

    /**
     * 메시지의 반응 목록 조회
     *
     * @param messageId 메시지 ID
     * @return 반응 목록
     */
    override fun getReactions(
        messageId: String
    ): Map<String, Set<String>> {
        val message = loadChatMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        return message.reactions
    }

    /**
     * WebSocket을 통해 반응 업데이트를 클라이언트에게 전송합니다.
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
                "isAdded" to isAdded
            )
        )
    }

}