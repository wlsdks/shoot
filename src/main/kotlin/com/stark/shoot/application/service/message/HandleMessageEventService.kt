package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.out.persistence.postgres.repository.OutboxEventRepository
import com.stark.shoot.application.port.`in`.message.HandleMessageEventUseCase
import com.stark.shoot.application.port.out.message.MessageStatusNotificationPort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.application.service.saga.message.MessageSagaOrchestrator
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.domain.event.type.EventType
import com.stark.shoot.domain.saga.SagaState
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 메시지 이벤트 처리 서비스 (Saga 패턴 적용)
 *
 * MongoDB + PostgreSQL 분산 트랜잭션을 Saga 패턴으로 처리합니다.
 * - Step 1: MongoDB 메시지 저장 (독립 트랜잭션)
 * - Step 2: PostgreSQL 채팅방 메타데이터 업데이트 (@Transactional 시작)
 * - Step 3: PostgreSQL Outbox 이벤트 저장 (Step 2의 트랜잭션에 참여)
 *
 * 트랜잭션 경계:
 * - Step 1은 독립적으로 실행 (MongoDB)
 * - Step 2, 3는 하나의 PostgreSQL 트랜잭션으로 묶임
 * - 실패 시 보상 트랜잭션이 자동으로 역순 실행
 */
@UseCase
class HandleMessageEventService(
    private val messageSagaOrchestrator: MessageSagaOrchestrator,
    private val loadUrlContentPort: LoadUrlContentPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val messageStatusNotificationPort: MessageStatusNotificationPort,
    private val outboxEventRepository: OutboxEventRepository
) : HandleMessageEventUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 Saga 패턴으로 저장하고 상태 업데이트를 전송합니다.
     *
     * 트랜잭션은 각 Step에서 관리:
     * - UpdateChatRoomMetadataStep: @Transactional 시작
     * - PublishEventToOutboxStep: @Transactional(MANDATORY)로 참여
     */
    override fun handle(event: MessageEvent): Boolean {
        if (event.type != EventType.MESSAGE_CREATED) return false

        val message = event.data
        val tempId = message.metadata.tempId

        return try {
            // Saga 실행: MongoDB 저장 → PostgreSQL 업데이트 → Outbox 저장
            val sagaContext = messageSagaOrchestrator.execute(message)

            // URL 미리보기 처리 (백그라운드, 실패해도 무시)
            processUrlPreviewIfNeeded(message)

            // Saga 결과에 따라 처리
            when (sagaContext.state) {
                SagaState.COMPLETED -> {
                    // 성공: 사용자에게 성공 알림
                    notifyPersistenceSuccess(sagaContext.savedMessage ?: message, tempId)
                    logger.info { "Message saga completed: sagaId=${sagaContext.sagaId}" }
                    true
                }

                SagaState.COMPENSATED, SagaState.FAILED -> {
                    // 실패: 보상 트랜잭션 완료 또는 실패
                    val error = Exception(sagaContext.error?.message ?: "Unknown saga error", sagaContext.error)
                    notifyPersistenceFailure(message, tempId, error)
                    logger.error { "Message saga failed: sagaId=${sagaContext.sagaId}, state=${sagaContext.state}" }
                    false
                }

                else -> {
                    logger.warn { "Unexpected saga state: ${sagaContext.state}" }
                    notifyPersistenceFailure(message, tempId, Exception("Unexpected saga state"))
                    false
                }
            }

        } catch (e: Exception) {
            logger.error(e) { "메시지 영속화 중 예외 발생: ${e.message}" }
            notifyPersistenceFailure(message, tempId, e)
            false
        }
    }

    /**
     * 영속화 성공을 사용자에게 알립니다.
     */
    private fun notifyPersistenceSuccess(
        message: ChatMessage,
        tempId: String?
    ) {
        if (tempId.isNullOrEmpty()) {
            logger.debug { "tempId가 없어서 영속화 성공 알림을 건너뜀: messageId=${message.id?.value}" }
            return
        }

        messageStatusNotificationPort.notifyMessageStatus(
            roomId = message.roomId.value,
            tempId = tempId,
            status = MessageStatus.SENT,
            errorMessage = null
        )

        logger.debug { "영속화 성공 알림 전송: roomId=${message.roomId.value}, tempId=$tempId" }
    }

    /**
     * 영속화 실패를 사용자에게 알립니다.
     */
    private fun notifyPersistenceFailure(
        message: ChatMessage,
        tempId: String?,
        exception: Exception
    ) {
        if (tempId.isNullOrEmpty()) {
            logger.warn { "tempId가 없어서 영속화 실패 알림을 보낼 수 없음: messageId=${message.id?.value}" }
            return
        }

        messageStatusNotificationPort.notifyMessageStatus(
            roomId = message.roomId.value,
            tempId = tempId,
            status = MessageStatus.FAILED,
            errorMessage = "영속화 실패: ${exception.message}"
        )

        logger.warn { "영속화 실패 알림 전송: roomId=${message.roomId.value}, tempId=$tempId" }
    }


    /**
     * URL 미리보기 처리 (필요시)
     */
    private fun processUrlPreviewIfNeeded(message: ChatMessage) {
        val previewUrl = message.metadata.previewUrl
        if (message.metadata.needsUrlPreview && previewUrl != null) {
            try {
                val preview = loadUrlContentPort.fetchUrlContent(previewUrl)

                if (preview != null) {
                    cacheUrlPreviewPort.cacheUrlPreview(previewUrl, preview)
                    // URL 미리보기 업데이트는 별도 이벤트로 처리
                }
            } catch (e: Exception) {
                logger.error(e) { "URL 미리보기 처리 실패: $previewUrl" }
            }
        }
    }

}
