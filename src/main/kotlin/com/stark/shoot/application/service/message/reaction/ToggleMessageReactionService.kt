package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.adapter.`in`.rest.dto.message.reaction.ReactionResponse
import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.reaction.ToggleMessageReactionUseCase
import com.stark.shoot.application.port.`in`.message.reaction.command.ToggleMessageReactionCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.reaction.MessageReactionCommandPort
import com.stark.shoot.application.port.out.message.reaction.MessageReactionQueryPort
import com.stark.shoot.domain.chat.reaction.MessageReaction
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.shared.event.MessageReactionEvent
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.redis.RedisLockManager
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.WebSocketResponseBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional

/**
 * 메시지 리액션 토글 서비스
 *
 * DDD Aggregate 분리:
 * - MessageReaction Aggregate를 별도로 관리
 * - ChatMessage와 독립적인 트랜잭션 처리
 * - 높은 동시성 처리 (Redis 분산 락 사용)
 */
@Transactional
@UseCase
class ToggleMessageReactionService(
    private val messageQueryPort: MessageQueryPort,
    private val messageReactionQueryPort: MessageReactionQueryPort,
    private val messageReactionCommandPort: MessageReactionCommandPort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val eventPublisher: EventPublishPort,
    private val redisLockManager: RedisLockManager
) : ToggleMessageReactionUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지에 리액션을 토글합니다.
     * 같은 리액션을 선택하면 제거하고, 다른 리액션을 선택하면 기존 리액션을 제거하고 새 리액션을 추가합니다.
     *
     * Redis 분산 락을 사용하여 동시성 문제를 방지합니다.
     * 메시지별로 독립적인 락을 사용하여 병렬성을 최대화합니다.
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

            // 메시지별 분산 락 획득하여 동시성 제어
            val lockKey = "message:reaction:${command.messageId.value}"
            val ownerId = "user:${command.userId.value}"

            return redisLockManager.withLock(lockKey, ownerId) {
                // 메시지 조회 (없으면 예외 발생)
                val message = messageQueryPort.findById(command.messageId)
                    ?: run {
                        sendErrorResponse(command.userId, "메시지를 찾을 수 없습니다.")
                        throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=${command.messageId}")
                    }

                // 기존 리액션 조회
                val existingReaction = messageReactionQueryPort.findByMessageIdAndUserId(
                    command.messageId,
                    command.userId
                )

                // 토글 처리
                val toggleResult = when {
                    // 1. 기존 리액션이 같은 타입 → 제거
                    existingReaction != null && existingReaction.reactionType == type -> {
                        messageReactionCommandPort.deleteByMessageIdAndUserId(
                            command.messageId,
                            command.userId
                        )
                        ToggleResult(
                            isAdded = false,
                            isReplacement = false,
                            previousReactionType = null
                        )
                    }

                    // 2. 기존 리액션이 다른 타입 → 교체
                    existingReaction != null -> {
                        val previousType = existingReaction.reactionType
                        existingReaction.changeReactionType(type)
                        messageReactionCommandPort.save(existingReaction)
                        ToggleResult(
                            isAdded = true,
                            isReplacement = true,
                            previousReactionType = previousType.code
                        )
                    }

                    // 3. 기존 리액션이 없음 → 추가
                    else -> {
                        val newReaction = MessageReaction.create(
                            messageId = command.messageId,
                            userId = command.userId,
                            reactionType = type
                        )
                        messageReactionCommandPort.save(newReaction)
                        ToggleResult(
                            isAdded = true,
                            isReplacement = false,
                            previousReactionType = null
                        )
                    }
                }

                // 최신 리액션 목록 조회
                val updatedReactions = messageReactionQueryPort.findAllByMessageId(command.messageId)
                    .groupBy { it.reactionType.code }
                    .mapValues { (_, reactions) ->
                        reactions.map { it.userId.value }.toSet()
                    }

                // 채팅방의 모든 참여자에게 반응 변경 알림
                webSocketMessageBroker.sendMessage(
                    "/topic/message/reaction/${message.roomId.value}",
                    mapOf(
                        "messageId" to command.messageId.value,
                        "reactions" to updatedReactions,
                        "userId" to command.userId.value,
                        "reactionType" to command.reactionType,
                        "action" to if (toggleResult.isAdded) "ADDED" else "REMOVED"
                    )
                )

                // 응답 생성
                val reactionResponse = ReactionResponse.from(
                    messageId = command.messageId.value,
                    reactions = updatedReactions,
                    updatedAt = java.time.Instant.now().toString()
                )

                sendSuccessResponse(command.userId, "반응이 업데이트되었습니다.", reactionResponse)

                // 이벤트 발행
                publishReactionEvents(
                    messageId = command.messageId,
                    roomId = message.roomId,
                    userId = command.userId,
                    reactionType = command.reactionType,
                    toggleResult = toggleResult
                )

                reactionResponse
            }

        } catch (e: Exception) {
            logger.error(e) { "메시지 반응 처리 중 오류 발생: ${e.message}" }
            sendErrorResponse(command.userId, "반응 처리 중 오류가 발생했습니다: ${e.message}")
            throw e
        }
    }

    private fun sendSuccessResponse(userId: UserId, message: String, data: ReactionResponse) {
        webSocketMessageBroker.sendMessage(
            "/queue/message/reaction/response/${userId.value}",
            WebSocketResponseBuilder.success(data, message)
        )
    }

    private fun sendErrorResponse(userId: UserId, message: String) {
        webSocketMessageBroker.sendMessage(
            "/queue/message/reaction/response/${userId.value}",
            WebSocketResponseBuilder.error(message)
        )
    }

    /**
     * 리액션 이벤트 발행
     */
    private fun publishReactionEvents(
        messageId: com.stark.shoot.domain.chat.message.vo.MessageId,
        roomId: com.stark.shoot.domain.chat.vo.ChatRoomId,
        userId: UserId,
        reactionType: String,
        toggleResult: ToggleResult
    ) {
        // 리액션 교체인 경우 2개의 이벤트 발행 (기존 제거 + 새로운 추가)
        if (toggleResult.isReplacement && toggleResult.previousReactionType != null) {
            // 기존 리액션 제거 이벤트
            eventPublisher.publishEvent(
                MessageReactionEvent.create(
                    messageId = messageId,
                    roomId = roomId,
                    userId = userId,
                    reactionType = toggleResult.previousReactionType,
                    isAdded = false,
                    isReplacement = true
                )
            )

            // 새 리액션 추가 이벤트
            eventPublisher.publishEvent(
                MessageReactionEvent.create(
                    messageId = messageId,
                    roomId = roomId,
                    userId = userId,
                    reactionType = reactionType,
                    isAdded = true,
                    isReplacement = true
                )
            )
        } else {
            // 일반 추가/제거 이벤트
            eventPublisher.publishEvent(
                MessageReactionEvent.create(
                    messageId = messageId,
                    roomId = roomId,
                    userId = userId,
                    reactionType = reactionType,
                    isAdded = toggleResult.isAdded,
                    isReplacement = false
                )
            )
        }
    }

    /**
     * 토글 결과를 나타내는 데이터 클래스
     */
    private data class ToggleResult(
        val isAdded: Boolean,
        val isReplacement: Boolean,
        val previousReactionType: String?
    )
}
