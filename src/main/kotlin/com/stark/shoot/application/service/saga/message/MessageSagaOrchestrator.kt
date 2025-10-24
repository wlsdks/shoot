package com.stark.shoot.application.service.saga.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.saga.SagaOrchestrator
import com.stark.shoot.domain.saga.message.MessageSagaContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * 메시지 저장 Saga 오케스트레이터
 *
 * MongoDB 메시지 저장 + PostgreSQL 채팅방 업데이트 + Outbox 이벤트 저장을
 * 원자적으로 처리하거나, 실패 시 보상 트랜잭션을 실행합니다.
 */
@Service
class MessageSagaOrchestrator(
    private val saveMessageStep: SaveMessageToMongoStep,
    private val updateChatRoomStep: UpdateChatRoomMetadataStep,
    private val publishEventStep: PublishEventToOutboxStep
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 저장 Saga 실행
     *
     * @param message 저장할 메시지
     * @return Saga 컨텍스트 (성공/실패 정보 포함)
     */
    fun execute(message: ChatMessage): MessageSagaContext {
        val context = MessageSagaContext(message = message)

        val orchestrator = SagaOrchestrator(
            listOf(
                saveMessageStep,        // 1. MongoDB에 메시지 저장
                updateChatRoomStep,     // 2. PostgreSQL에 채팅방 메타데이터 업데이트
                publishEventStep        // 3. Outbox에 이벤트 저장 (PostgreSQL)
            )
        )

        val success = orchestrator.execute(context)

        if (success) {
            context.markCompleted()
            logger.info { "Message saga completed successfully: sagaId=${context.sagaId}" }
        } else {
            logger.error { "Message saga failed: sagaId=${context.sagaId}, error=${context.error?.message}" }
        }

        return context
    }
}
