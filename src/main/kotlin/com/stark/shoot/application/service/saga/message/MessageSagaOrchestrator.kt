package com.stark.shoot.application.service.saga.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.AlertLevel
import com.stark.shoot.application.port.out.AlertNotificationPort
import com.stark.shoot.application.port.out.CriticalAlert
import com.stark.shoot.application.port.out.DeadLetterMessage
import com.stark.shoot.application.port.out.DeadLetterQueuePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.saga.SagaOrchestrator
import com.stark.shoot.domain.saga.SagaState
import com.stark.shoot.domain.saga.message.MessageSagaContext
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.OptimisticLockException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 메시지 저장 Saga 오케스트레이터
 *
 * MongoDB 메시지 저장 + PostgreSQL 채팅방 업데이트 + Outbox 이벤트 저장을
 * 원자적으로 처리하거나, 실패 시 보상 트랜잭션을 실행합니다.
 *
 * OptimisticLockException 재시도:
 * - Step 레벨이 아닌 Orchestrator 레벨에서 재시도
 * - 각 재시도마다 새로운 트랜잭션이 시작되어 JPA 1차 캐시 문제 해결
 */
@Service
class MessageSagaOrchestrator(
    private val saveMessageStep: SaveMessageToMongoStep,
    private val updateChatRoomStep: UpdateChatRoomMetadataStep,
    private val publishEventStep: PublishEventToOutboxStep,
    private val deadLetterQueuePort: DeadLetterQueuePort? = null,  // Optional: DLQ가 설정되지 않으면 null
    private val alertNotificationPort: AlertNotificationPort? = null,  // Optional: 알림이 설정되지 않으면 null
    private val objectMapper: ObjectMapper,
    @Value("\${notification.slack.channel.critical:#ops-critical}")
    private val criticalChannel: String = "#ops-critical"
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val MAX_RETRIES = 3

        /**
         * Exponential Backoff 계산
         * 재시도 횟수에 따라 대기 시간 증가: 0ms, 10ms, 100ms
         */
        private fun calculateBackoff(attempt: Int): Long {
            return when (attempt) {
                1 -> 0L      // 첫 재시도: 즉시
                2 -> 10L     // 두 번째: 10ms
                else -> 100L // 세 번째: 100ms
            }
        }
    }

    /**
     * 메시지 저장 Saga 실행
     *
     * OptimisticLockException 발생 시 최대 3회 재시도합니다.
     * 각 재시도마다 새로운 트랜잭션이 시작되므로 JPA 1차 캐시 문제가 없습니다.
     *
     * DDD 개선: Context에는 ID만 전달, Step에서 메시지 객체 직접 주입
     *
     * @param message 저장할 메시지
     * @return Saga 컨텍스트 (성공/실패 정보 포함)
     */
    fun execute(message: ChatMessage): MessageSagaContext {
        var attempt = 0

        while (attempt < MAX_RETRIES) {
            // DDD 개선: Context에는 ID와 primitive만 전달
            val context = MessageSagaContext(
                messageId = message.id,
                roomId = message.roomId,
                senderId = message.senderId
            )

            // Step들이 접근할 수 있도록 메시지 전달 (임시 저장소)
            saveMessageStep.setMessage(message)

            try {
                val success = executeInternal(context)

                if (success) {
                    context.markCompleted()
                    logger.info { "Message saga completed successfully: sagaId=${context.sagaId}" }
                    return context
                } else if (context.error is OptimisticLockException && attempt < MAX_RETRIES - 1) {
                    // OptimisticLockException인 경우에만 재시도
                    attempt++
                    val backoffMs = calculateBackoff(attempt)
                    logger.warn {
                        "OptimisticLockException occurred, retrying after ${backoffMs}ms... " +
                                "(attempt $attempt/$MAX_RETRIES)"
                    }

                    if (backoffMs > 0) {
                        runCatching { Thread.sleep(backoffMs) }
                    }
                    continue
                } else {
                    // 다른 오류이거나 최대 재시도 횟수 도달
                    logger.error {
                        "Message saga failed: sagaId=${context.sagaId}, " +
                                "error=${context.error?.message}"
                    }
                    return context
                }
            } catch (e: Exception) {
                logger.error(e) { "Unexpected error in saga orchestration" }
                context.markFailed(e)
                return context
            }
        }

        // 모든 재시도 실패 (여기에 도달하지 않아야 함)
        val context = MessageSagaContext(
            messageId = message.id,
            roomId = message.roomId,
            senderId = message.senderId
        )
        context.markFailed(Exception("All retries failed"))
        return context
    }

    private fun executeInternal(context: MessageSagaContext): Boolean {
        val orchestrator = SagaOrchestrator(
            listOf(
                saveMessageStep,        // 1. MongoDB에 메시지 저장
                updateChatRoomStep,     // 2. PostgreSQL에 채팅방 메타데이터 업데이트
                publishEventStep        // 3. Outbox에 이벤트 저장 (PostgreSQL)
            )
        )

        val success = orchestrator.execute(context)

        // 보상 실패 시 알림 발송
        if (!success && context.state == SagaState.FAILED) {
            handleCompensationFailure(context)
        }

        return success
    }

    /**
     * 보상 실패 시 Dead Letter Queue 발행 및 Critical 알림 전송
     *
     * @param context 실패한 Saga 컨텍스트
     */
    private fun handleCompensationFailure(context: MessageSagaContext) {
        try {
            val failedSteps = listOf(
                "SaveMessageToMongoStep",
                "UpdateChatRoomMetadataStep",
                "PublishEventToOutboxStep"
            )

            // Context를 JSON으로 직렬화
            val payload = runCatching {
                objectMapper.writeValueAsString(
                    mapOf(
                        "sagaId" to context.sagaId,
                        "messageId" to context.messageId?.value,
                        "roomId" to context.roomId.value,
                        "senderId" to context.senderId.value,
                        "state" to context.state.name,
                        "savedMessageId" to context.savedMessageId,
                        "chatRoomSnapshot" to context.chatRoomSnapshot
                    )
                )
            }.getOrElse { "{\"error\": \"Failed to serialize context\"}" }

            // 1. Dead Letter Queue 발행
            deadLetterQueuePort?.publish(
                DeadLetterMessage(
                    sagaId = context.sagaId,
                    sagaType = "MessageSaga",
                    failedSteps = failedSteps,
                    errorDetails = context.error?.message ?: "Unknown error",
                    payload = payload,
                    requiresManualIntervention = true
                )
            )

            // 2. Slack Critical 알림 전송
            alertNotificationPort?.sendCriticalAlert(
                CriticalAlert(
                    channel = criticalChannel,
                    title = "🚨 Message Saga Compensation Failed",
                    message = buildAlertMessage(context),
                    level = AlertLevel.CRITICAL,
                    metadata = mapOf(
                        "sagaId" to context.sagaId,
                        "messageId" to (context.messageId?.value?.toString() ?: "null"),
                        "roomId" to context.roomId.value.toString(),
                        "error" to (context.error?.message ?: "Unknown")
                    )
                )
            )

            logger.error {
                "Saga compensation failed - DLQ and alert sent: sagaId=${context.sagaId}, " +
                        "error=${context.error?.message}"
            }
        } catch (e: Exception) {
            // 알림 전송 실패해도 로그만 남기고 계속 진행
            logger.error(e) { "Failed to send compensation failure notification for sagaId=${context.sagaId}" }
        }
    }

    /**
     * Slack 알림 메시지 생성
     */
    private fun buildAlertMessage(context: MessageSagaContext): String {
        return """
            *Saga ID*: `${context.sagaId}`
            *Message ID*: `${context.messageId?.value ?: "null"}`
            *Room ID*: `${context.roomId.value}`
            *Sender ID*: `${context.senderId.value}`
            *State*: `${context.state}`
            *Error*: ${context.error?.message ?: "Unknown error"}

            ⚠️ *Manual intervention required*
            Check Dead Letter Queue for details: `dead-letter-queue` topic
        """.trimIndent()
    }
}
