package com.stark.shoot.application.service.saga.message

import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.service.util.*
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
     * DDD 개선: Context에서 savedMessageId를 읽어 메시지 조회
     *
     * OptimisticLockException은 MessageSagaOrchestrator 레벨에서 처리됩니다.
     * 각 재시도마다 새로운 트랜잭션이 시작되므로 JPA 1차 캐시 문제가 해결됩니다.
     */
    @Transactional  // PostgreSQL 트랜잭션 시작
    override fun execute(context: MessageSagaContext): Boolean {
        val messageIdStr = context.savedMessageId
            ?: throw IllegalStateException("Message not saved yet")

        return try {
            executeInternal(context, messageIdStr)
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
        messageIdStr: String
    ): Boolean {
        // 채팅방 조회
        val chatRoom = chatRoomQueryPort.findById(context.roomId.toChatRoom())
        if (chatRoom == null) {
            logger.error { "ChatRoom not found: roomId=${context.roomId.value}" }
            return false
        }

        // 보상용 스냅샷 저장
        context.chatRoomSnapshot = MessageSagaContext.ChatRoomSnapshot(
            roomId = chatRoom.id!!.value,
            previousLastMessageId = chatRoom.lastMessageId,
            previousLastActiveAt = chatRoom.lastActiveAt
        )

        // 메타데이터 업데이트 (DDD 개선: messageId와 createdAt만 전달)
        val updatedRoom = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(
            chatRoom = chatRoom,
            messageId = messageIdStr,
            createdAt = java.time.Instant.now()
        )
        val savedRoom = chatRoomCommandPort.save(updatedRoom)

        // 마지막 읽은 메시지 ID 업데이트
        val messageId = com.stark.shoot.domain.chat.message.vo.MessageId.from(messageIdStr)
        chatRoomCommandPort.updateLastReadMessageId(
            context.roomId.toChatRoom(),
            context.senderId,
            messageId
        )

        context.recordStep(stepName())

        logger.info { "ChatRoom metadata updated: roomId=${savedRoom.id?.value}" }
        return true
    }

    @Transactional  // 보상 트랜잭션
    override fun compensate(context: MessageSagaContext): Boolean {
        return try {
            val snapshot = context.chatRoomSnapshot

            if (snapshot == null) {
                logger.warn { "No chatroom snapshot to restore - skipping compensation" }
                return true
            }

            // DB에서 최신 상태의 채팅방을 다시 조회 (version=N+1)
            val roomId = com.stark.shoot.domain.chatroom.vo.ChatRoomId.from(snapshot.roomId)
            val currentRoom = chatRoomQueryPort.findById(roomId)
            if (currentRoom == null) {
                logger.warn { "ChatRoom not found for compensation: roomId=${snapshot.roomId}" }
                return true  // 채팅방이 이미 삭제됨 - 보상 불필요
            }

            // 스냅샷 데이터로 이전 상태 복원
            currentRoom.update(
                lastMessageId = snapshot.previousLastMessageId,
                lastActiveAt = snapshot.previousLastActiveAt
            )

            chatRoomCommandPort.save(currentRoom)
            logger.info { "Compensated: Restored chatroom metadata: roomId=${snapshot.roomId}" }
            true
        } catch (e: OptimisticLockException) {
            // OptimisticLockException 발생 시 재시도
            logger.warn(e) { "OptimisticLockException during compensation - retrying once" }

            try {
                // 한 번 더 재시도
                val snapshot = context.chatRoomSnapshot ?: return false
                val roomId = com.stark.shoot.domain.chatroom.vo.ChatRoomId.from(snapshot.roomId)
                val currentRoom = chatRoomQueryPort.findById(roomId) ?: return true

                currentRoom.update(
                    lastMessageId = snapshot.previousLastMessageId,
                    lastActiveAt = snapshot.previousLastActiveAt
                )

                chatRoomCommandPort.save(currentRoom)
                logger.info { "Compensated after retry: roomId=${snapshot.roomId}" }
                true
            } catch (retryException: Exception) {
                logger.error(retryException) { "Compensation failed after retry - manual intervention required" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to compensate chatroom metadata update" }
            false
        }
    }

    override fun stepName() = "UpdateChatRoomMetadata"
}
