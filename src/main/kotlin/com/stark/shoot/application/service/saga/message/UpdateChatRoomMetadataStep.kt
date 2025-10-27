package com.stark.shoot.application.service.saga.message

import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.saga.SagaStep
import com.stark.shoot.domain.saga.message.MessageSagaContext
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.OptimisticLockException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Saga Step 2: PostgreSQL 채팅방 메타데이터 업데이트
 *
 * @Transactional: PostgreSQL 트랜잭션 시작
 * Step 3(Outbox)도 이 트랜잭션에 참여하여 함께 커밋/롤백됩니다.
 *
 * Optimistic Locking:
 * ChatRoomEntity에 @Version이 있어 동시 업데이트 시 OptimisticLockException 발생
 * 최대 3회 재시도하여 동시성 문제 해결
 */
@Component
class UpdateChatRoomMetadataStep(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService
) : SagaStep<MessageSagaContext> {

    private val logger = KotlinLogging.logger {}

    /**
     * ChatRoom 메타데이터 업데이트 실행
     *
     * OptimisticLockException은 MessageSagaOrchestrator 레벨에서 처리됩니다.
     * 각 재시도마다 새로운 트랜잭션이 시작되므로 JPA 1차 캐시 문제가 해결됩니다.
     */
    @Transactional  // PostgreSQL 트랜잭션 시작
    override fun execute(context: MessageSagaContext): Boolean {
        val savedMessage = context.savedMessage
            ?: throw IllegalStateException("Message not saved yet")

        return try {
            executeInternal(context, savedMessage)
        } catch (e: OptimisticLockException) {
            // Orchestrator 레벨에서 재시도하도록 예외를 context에 저장하고 실패 반환
            logger.warn { "OptimisticLockException occurred - will be retried by orchestrator" }
            context.markFailed(e)
            false
        } catch (e: Exception) {
            logger.error(e) { "Failed to update chatroom metadata" }
            context.markFailed(e)
            false
        }
    }

    private fun executeInternal(
        context: MessageSagaContext,
        savedMessage: com.stark.shoot.domain.chat.message.ChatMessage
    ): Boolean {
        // 채팅방 조회
        val chatRoom = chatRoomQueryPort.findById(savedMessage.roomId)
        if (chatRoom == null) {
            logger.error { "ChatRoom not found: roomId=${savedMessage.roomId.value}" }
            return false
        }

        context.chatRoom = chatRoom

        // 메타데이터 업데이트
        val updatedRoom = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(chatRoom, savedMessage)
        val savedRoom = chatRoomCommandPort.save(updatedRoom)

        // 마지막 읽은 메시지 ID 업데이트
        savedMessage.id?.let { messageId ->
            chatRoomCommandPort.updateLastReadMessageId(
                savedMessage.roomId,
                savedMessage.senderId,
                messageId
            )
        }

        context.updatedChatRoom = savedRoom
        context.recordStep(stepName())

        logger.info { "ChatRoom metadata updated: roomId=${savedRoom.id?.value}" }
        return true
    }

    override fun compensate(context: MessageSagaContext): Boolean {
        return try {
            val originalRoom = context.chatRoom
            if (originalRoom != null) {
                // 원래 상태로 복원
                // 주의: 보상 트랜잭션 시 OptimisticLockException 발생 가능
                // - 이미 버전이 증가한 상태에서 원본을 복원하려고 하면 충돌
                // - 하지만 보상 시점에는 다른 동시 트랜잭션이 없으므로 대부분 성공
                // - 실패 시 Saga 상태가 COMPENSATED가 아닌 FAILED로 표시되어 수동 개입 필요
                chatRoomCommandPort.save(originalRoom)
                logger.info { "Compensated: Restored chatroom metadata: roomId=${originalRoom.id?.value}" }
            } else {
                logger.warn { "No original chatroom to restore - skipping compensation" }
            }
            true
        } catch (e: OptimisticLockException) {
            // OptimisticLockException은 보상 실패로 간주
            // 데이터 불일치 상태이므로 수동 개입 필요
            logger.error(e) { "Compensation failed due to OptimisticLockException - manual intervention required" }
            false
        } catch (e: Exception) {
            logger.error(e) { "Failed to compensate chatroom metadata update" }
            false
        }
    }

    override fun stepName() = "UpdateChatRoomMetadata"
}
