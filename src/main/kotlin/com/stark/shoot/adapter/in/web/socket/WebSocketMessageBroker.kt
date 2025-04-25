package com.stark.shoot.adapter.`in`.web.socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.concurrent.*

@Component
class WebSocketMessageBroker(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    // 메시지 배치 처리를 위한 버퍼
    private val messageBuffer = ConcurrentHashMap<String, MutableList<Any>>()

    // 배치 처리를 위한 스케줄러
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2).apply {
        scheduleAtFixedRate(::flushMessageBuffer, 50, 50, TimeUnit.MILLISECONDS)
    }

    // 메시지 전송 성공/실패 메트릭
    private var messagesSent = 0
    private var messagesFailed = 0

    @PreDestroy
    fun onApplicationShutdown() {
        logger.info { "애플리케이션 종료: WebSocket 메시지 브로커 리소스 정리 중..." }
        shutdown()
    }

    /**
     * WebSocket을 통해 메시지를 전송합니다.
     * - 메시지를 버퍼에 추가하고 배치로 처리합니다.
     * - 실패한 메시지는 Redis에 저장합니다.
     *
     * @param destination 전송할 WebSocket의 목적지
     * @param payload 전송할 메시지
     * @param retryCount 재시도 횟수 (기본값: 3)
     */
    @Async
    fun sendMessage(destination: String, payload: Any, retryCount: Int = 3): CompletableFuture<Boolean> {
        // 메시지를 버퍼에 추가
        messageBuffer.computeIfAbsent(destination) { mutableListOf() }.add(payload)

        // 현재 버퍼 크기를 확인
        val currentBufferSize = messageBuffer[destination]?.size ?: 0

        // 버퍼 크기가 10개 이상이면 즉시 플러시
        if (currentBufferSize >= 10) {
            flushDestinationBuffer(destination)
        }

        // 배치 전송 시도
        return CompletableFuture.completedFuture(true)
    }

    /**
     * 특정 대상에 대한 메시지 버퍼를 플러시합니다.
     */
    private fun flushDestinationBuffer(destination: String) {
        val messages = messageBuffer[destination]?.toList() ?: return
        if (messages.isEmpty()) return

        // 버퍼에서 메시지 제거
        messageBuffer.remove(destination)

        // 배치 전송 시도
        sendBatchedMessages(destination, messages)
    }

    /**
     * 모든 메시지 버퍼를 플러시합니다.
     */
    private fun flushMessageBuffer() {
        try {
            val destinations = messageBuffer.keys.toList()
            for (destination in destinations) {
                flushDestinationBuffer(destination)
            }

            // 주기적으로 메트릭 로깅
            if (messagesSent > 0 || messagesFailed > 0) {
                logger.info { "WebSocket 메시지 통계: 성공=$messagesSent, 실패=$messagesFailed" }
                messagesSent = 0
                messagesFailed = 0
            }
        } catch (e: Exception) {
            logger.error(e) { "메시지 버퍼 플러시 중 오류 발생" }
        }
    }

    /**
     * 배치로 메시지를 전송합니다.
     */
    private fun sendBatchedMessages(
        destination: String,
        messages: List<Any>,
        retryCount: Int = 3
    ) {
        var attempt = 0
        var success = false

        while (!success && attempt < retryCount) {
            try {
                if (messages.size == 1) {
                    // 단일 메시지인 경우 일반 전송
                    simpMessagingTemplate.convertAndSend(destination, messages[0])
                } else {
                    // 여러 메시지인 경우 배치 전송
                    simpMessagingTemplate.convertAndSend(destination, messages)
                }
                success = true
                messagesSent += messages.size
            } catch (e: Exception) {
                attempt++
                logger.error(e) { "WebSocket 메시지 전송 실패: $destination, 메시지 수: ${messages.size}, 시도 횟수: $attempt" }
                if (attempt < retryCount) {
                    // 지수 백오프 - 비차단 방식으로 변경
                    try {
                        TimeUnit.MILLISECONDS.sleep(100 * (1L shl attempt))
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
        }

        if (!success) {
            messagesFailed += messages.size
            // 최대 재시도 횟수 초과 시 Redis에 메시지 저장 (파이프라인 사용)
            try {
                redisTemplate.executePipelined { connection ->
                    for (payload in messages) {
                        val key = when (payload) {
                            is MessageStatusResponse -> "failed-message-tempId:${payload.tempId}"
                            else -> "failed-message:${System.currentTimeMillis()}"
                        }
                        val value = objectMapper.writeValueAsString(payload)
                        connection.stringCommands().set(key.toByteArray(), value.toByteArray())
                    }
                    null
                }
            } catch (e: Exception) {
                logger.error(e) { "Redis에 실패한 메시지 저장 중 오류 발생" }
            }
            logger.error { "WebSocket 메시지 전송 실패: $destination, 메시지 수: ${messages.size}, 최대 재시도 횟수 초과" }
        }
    }

    /**
     * 애플리케이션 종료 시 리소스 정리
     */
    fun shutdown() {
        try {
            // 남은 메시지 처리
            flushMessageBuffer()

            // 스케줄러 종료
            scheduler.shutdown()
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: Exception) {
            logger.error(e) { "WebSocket 메시지 브로커 종료 중 오류 발생" }
            scheduler.shutdownNow()
        }
    }

}
