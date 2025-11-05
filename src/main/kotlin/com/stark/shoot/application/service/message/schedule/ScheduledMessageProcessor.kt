package com.stark.shoot.application.service.message.schedule

import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.toRequestDto
import com.stark.shoot.application.port.out.message.MessagePublisherPort
import com.stark.shoot.application.port.out.message.ScheduledMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.ScheduledMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.config.redis.RedisLockManager
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import jakarta.annotation.PreDestroy
import java.util.concurrent.Future

@Component
class ScheduledMessageProcessor(
    private val scheduledMessagePort: ScheduledMessagePort,
    private val messagePublisherPort: MessagePublisherPort,
    private val redisLockManager: RedisLockManager
) {
    private val logger = KotlinLogging.logger {}

    // 병렬 처리를 위한 스레드 풀 (CPU 코어 수 기반으로 설정)
    private val executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    )

    // 최대 재시도 횟수
    private val maxRetries = 3

    // 재시도 간격 (밀리초)
    private val retryDelayMs = 1000L

    /**
     * 10초마다 실행되는 스케줄러
     * 예약 시간이 되었거나 지난 메시지를 처리합니다.
     */
    @Scheduled(fixedRate = 10000) // 10초마다 실행
    fun processScheduledMessages() {
        logger.debug { "예약 메시지 처리 작업 실행" }

        val now = Instant.now()
        val messagesToProcess = scheduledMessagePort.findPendingMessagesBeforeTime(now)

        if (messagesToProcess.isEmpty()) {
            logger.debug { "처리할 예약 메시지가 없습니다" }
            return
        }

        logger.info { "처리할 예약 메시지: ${messagesToProcess.size}개" }

        // 메시지를 배치로 처리 (병렬 처리)
        processMessagesBatch(messagesToProcess)
    }

    /**
     * 메시지 배치 처리
     * 여러 메시지를 병렬로 처리합니다.
     */
    private fun processMessagesBatch(messages: List<ScheduledMessage>) {
        // 각 메시지를 별도 스레드에서 처리하고 Future로 추적
        val futures = messages.map { message ->
            executorService.submit {
                // 분산 락을 사용하여 동일 메시지의 중복 처리 방지
                val lockKey = "scheduled-message:${message.id}"
                val ownerId = "processor-${UUID.randomUUID()}"

                try {
                    redisLockManager.withLock(lockKey, ownerId) {
                        // 메시지 상태 재확인 (다른 인스턴스에서 이미 처리했을 수 있음)
                        val currentMessage = message.id?.let { scheduledMessagePort.findById(it.value.toObjectId()) }
                        if (currentMessage != null && currentMessage.status == ScheduledMessageStatus.PENDING) {
                            processMessageWithRetry(currentMessage)
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "예약 메시지 락 획득 실패: ${message.id}" }
                }
            }
        }

        // 모든 작업이 완료될 때까지 최대 30초 대기 (스레드 풀은 재사용)
        waitForCompletion(futures)
    }

    /**
     * 모든 Future 작업의 완료를 기다립니다.
     * 스레드 풀을 종료하지 않고 재사용합니다.
     */
    private fun waitForCompletion(futures: List<Future<*>>) {
        val startTime = System.currentTimeMillis()
        val timeoutMs = 30_000L // 30초
        
        try {
            for (future in futures) {
                val remainingTime = timeoutMs - (System.currentTimeMillis() - startTime)
                if (remainingTime <= 0) {
                    logger.warn { "예약 메시지 처리 시간 초과, 남은 작업들을 취소합니다." }
                    futures.forEach { it.cancel(true) }
                    break
                }
                
                try {
                    future.get(remainingTime, TimeUnit.MILLISECONDS)
                } catch (e: Exception) {
                    logger.error(e) { "예약 메시지 처리 중 오류 발생: ${e.message}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "예약 메시지 배치 처리 중 예외 발생" }
            futures.forEach { future ->
                try {
                    future.cancel(true)
                } catch (cancelEx: Exception) {
                    logger.debug(cancelEx) { "Future 취소 중 오류 발생" }
                }
            }
        }
    }

    /**
     * 애플리케이션 종료 시 스레드 풀을 정리합니다.
     */
    @PreDestroy
    fun shutdown() {
        logger.info { "ScheduledMessageProcessor 종료 중..." }
        try {
            executorService.shutdown()
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn { "스레드 풀이 정상적으로 종료되지 않아 강제 종료합니다." }
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
            logger.error(e) { "스레드 풀 종료 중 인터럽트 발생" }
        }
    }

    /**
     * 개별 예약 메시지 처리 (재시도 로직 포함)
     *
     * 부분 실패 방지 메커니즘:
     * 1. 처리 시작 전 상태를 PROCESSING으로 변경 (중복 발송 방지)
     * 2. 메시지 발행 시도
     * 3. 성공 시 SENT, 실패 시 FAILED로 변경
     *
     * 만약 발행은 성공했는데 상태 업데이트가 실패하더라도:
     * - 메시지는 PROCESSING 상태로 남음
     * - 다음 스케줄러 실행 시 PENDING만 조회하므로 재발송되지 않음
     */
    private fun processMessageWithRetry(message: ScheduledMessage, retryCount: Int = 0) {
        try {
            // Step 1: 상태를 PROCESSING으로 먼저 변경 (중복 발송 방지)
            if (retryCount == 0) {
                val processingMessage = message.copy(status = ScheduledMessageStatus.PROCESSING)
                scheduledMessagePort.saveScheduledMessage(processingMessage)
                logger.debug { "예약 메시지 상태를 PROCESSING으로 변경: ${message.id}" }
            }

            // Step 2: 메시지 요청 객체 생성
            val chatMessageRequest = createChatMessageRequest(message)

            // Step 3: 도메인 메시지 객체 생성
            val chatMessage = createChatMessage(message)

            // Step 4: 메시지 발행 (Redis, Kafka)
            messagePublisherPort.publish(chatMessageRequest, chatMessage)

            // Step 5: 상태를 SENT로 업데이트
            val updatedMessage = message.copy(status = ScheduledMessageStatus.SENT)
            scheduledMessagePort.saveScheduledMessage(updatedMessage)

            logger.info { "예약 메시지 전송 성공: ${message.id}" }
        } catch (e: Exception) {
            logger.error(e) { "예약 메시지 전송 실패 (시도 ${retryCount + 1}/${maxRetries}): ${message.id}" }

            // 재시도 로직
            if (retryCount < maxRetries - 1) {
                try {
                    // 재시도 전 지연
                    Thread.sleep(retryDelayMs)
                    // 재귀적으로 재시도
                    processMessageWithRetry(message, retryCount + 1)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    logger.error(ie) { "재시도 중 인터럽트 발생: ${message.id}" }
                    // 인터럽트 발생 시에도 상태를 FAILED로 변경
                    updateStatusSafely(message, ScheduledMessageStatus.FAILED)
                }
            } else {
                // 최대 재시도 횟수 초과 시 상태를 FAILED로 변경
                logger.error { "최대 재시도 횟수 초과, 예약 메시지 처리 실패: ${message.id}" }
                updateStatusSafely(message, ScheduledMessageStatus.FAILED)
            }
        }
    }

    /**
     * 예외가 발생해도 안전하게 상태를 업데이트합니다.
     */
    private fun updateStatusSafely(message: ScheduledMessage, status: ScheduledMessageStatus) {
        try {
            val updatedMessage = message.copy(status = status)
            scheduledMessagePort.saveScheduledMessage(updatedMessage)
            logger.info { "예약 메시지 상태를 ${status}로 변경: ${message.id}" }
        } catch (e: Exception) {
            logger.error(e) { "예약 메시지 상태 업데이트 실패: ${message.id}, status=$status" }
        }
    }

    /**
     * ScheduledMessage에서 ChatMessageRequest 객체 생성
     */
    private fun createChatMessageRequest(scheduledMessage: ScheduledMessage): ChatMessageRequest {
        return ChatMessageRequest(
            roomId = scheduledMessage.roomId,
            senderId = scheduledMessage.senderId,
            content = MessageContentRequest(
                text = scheduledMessage.content.text,
                type = scheduledMessage.content.type
            ),
            tempId = UUID.randomUUID().toString(),
            metadata = scheduledMessage.metadata.toRequestDto()
        )
    }

    /**
     * ScheduledMessage에서 ChatMessage 도메인 객체 생성
     */
    private fun createChatMessage(scheduledMessage: ScheduledMessage): ChatMessage {
        return ChatMessage(
            id = MessageId.from(UUID.randomUUID().toString()),
            roomId = ChatRoomId.from(scheduledMessage.roomId),
            senderId = UserId.from(scheduledMessage.senderId),
            content = scheduledMessage.content,
            status = MessageStatus.SENT, // 예약 메시지는 실행 시점에 이미 처리 완료된 상태
            metadata = scheduledMessage.metadata,
            createdAt = Instant.now()
        )
    }
}
