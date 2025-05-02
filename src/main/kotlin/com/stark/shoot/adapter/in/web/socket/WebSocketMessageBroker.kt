package com.stark.shoot.adapter.`in`.web.socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

@Component
class WebSocketMessageBroker(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val coroutineScope: ApplicationCoroutineScope
) {
    private val logger = KotlinLogging.logger {}

    // 메시지 배치 처리를 위한 버퍼 (스레드 안전)
    private val messageBuffer = ConcurrentHashMap<String, ConcurrentLinkedQueue<Any>>()

    // 배치 처리를 위한 스케줄러
    private val scheduler: ScheduledExecutorService =
        Executors.newScheduledThreadPool(2).apply {
            scheduleAtFixedRate(
                ::flushMessageBuffer,
                50,
                50,
                TimeUnit.MILLISECONDS
            )
        }

    // 메시지 전송 성공/실패 메트릭 (스레드 안전)
    private val messagesSent = AtomicInteger(0)
    private val messagesFailed = AtomicInteger(0)

    // 메시지 전송 타임아웃 (밀리초)
    private val MESSAGE_SEND_TIMEOUT = 5000L
    private val MAX_QUEUE_SIZE = 1000 // 적절한 값으로 조정

    @PreDestroy
    fun onApplicationShutdown() {
        logger.info { "애플리케이션 종료: WebSocket 메시지 브로커 리소스 정리 중..." }
        shutdown()
    }

    /**
     * WebSocket을 통해 메시지를 전송합니다.
     * - 메시지를 버퍼에 추가하고 배치로 처리합니다.
     * - 실패한 메시지는 Redis에 저장합니다.
     * - 코루틴을 사용하여 비동기적으로 처리합니다.
     * - 타임아웃을 적용하여 무한 대기를 방지합니다.
     *
     * @param destination 전송할 WebSocket의 목적지
     * @param payload 전송할 메시지
     * @param retryCount 재시도 횟수 (기본값: 3)
     * @return 메시지 전송 성공 여부를 담은 CompletableFuture
     */
    fun sendMessage(destination: String, payload: Any, retryCount: Int = 3): CompletableFuture<Boolean> {
        val result = CompletableFuture<Boolean>()

        try {
            // 메시지를 버퍼에 추가 (스레드 안전하게)
            val queue = messageBuffer.computeIfAbsent(destination) {
                ConcurrentLinkedQueue<Any>()
            }
            queue.add(payload)

            // 현재 큐 크기 확인
            val currentQueueSize = queue.size

            // 큐 크기가 최대 크기 이상이면 메시지 전송 거부
            if (currentQueueSize >= MAX_QUEUE_SIZE) {
                logger.warn { "대상 $destination 의 메시지 큐가 최대 크기($MAX_QUEUE_SIZE)에 도달했습니다. 새 메시지 거부." }
                result.complete(false)
                return result
            }

            // 코루틴을 사용하여 비동기적으로 처리
            coroutineScope.launch {
                try {
                    // 타임아웃 적용
                    withTimeout(MESSAGE_SEND_TIMEOUT) {
                        // 큐 크기가 10개 이상이면 즉시 플러시
                        if (currentQueueSize >= 10) {
                            flushDestinationBuffer(destination)
                        }
                        result.complete(true)
                    }
                } catch (e: TimeoutCancellationException) {
                    logger.error(e) { "WebSocket 메시지 전송 타임아웃: $destination" }
                    result.complete(false)
                } catch (e: Exception) {
                    logger.error(e) { "WebSocket 메시지 전송 중 오류 발생: $destination" }
                    result.complete(false)
                }
            }.invokeOnCompletion { throwable ->
                throwable?.let {
                    logger.error(it) { "코루틴 처리중 예외 발생" }
                    if (!result.isDone) result.complete(false)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "WebSocket 메시지 버퍼링 중 오류 발생: $destination" }
            result.complete(false)
        }

        return result
    }

    /**
     * 특정 대상에 대한 메시지 버퍼를 플러시합니다.
     */
    private fun flushDestinationBuffer(destination: String) {
        val queue = messageBuffer[destination] ?: return
        if (queue.isEmpty()) return

        // 큐에서 모든 메시지를 가져와 리스트로 변환
        val messages = mutableListOf<Any>()
        var message: Any?

        while (queue.poll().also { message = it } != null) {
            message?.let { messages.add(it) }
        }

        if (messages.isEmpty()) return

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
            if (messagesSent.get() > 0 || messagesFailed.get() > 0) {
                logger.debug { "WebSocket 메시지 통계: 성공=${messagesSent.get()}, 실패=${messagesFailed.get()}" }
                messagesSent.set(0)
                messagesFailed.set(0)
            }
        } catch (e: Exception) {
            logger.error(e) { "메시지 버퍼 플러시 중 오류 발생" }
        }
    }

    /**
     * 배치로 메시지를 전송합니다.
     * 코루틴을 사용하여 비동기적으로 처리하고 타임아웃을 적용합니다.
     */
    private fun sendBatchedMessages(
        destination: String,
        messages: List<Any>,
        retryCount: Int = 3
    ) {
        coroutineScope.launch {
            var attempt = 0
            var success = false

            while (!success && attempt < retryCount) {
                try {
                    // 타임아웃 적용
                    withTimeout(MESSAGE_SEND_TIMEOUT) {
                        if (messages.size == 1) {
                            // 단일 메시지인 경우 일반 전송
                            simpMessagingTemplate.convertAndSend(destination, messages[0])
                        } else {
                            // 여러 메시지인 경우 배치 전송
                            simpMessagingTemplate.convertAndSend(destination, messages)
                        }
                    }
                    success = true
                    messagesSent.addAndGet(messages.size)
                } catch (e: TimeoutCancellationException) {
                    attempt++
                    logger.error(e) { "WebSocket 메시지 전송 타임아웃: $destination, 메시지 수: ${messages.size}, 시도 횟수: $attempt" }
                    if (attempt < retryCount) {
                        // 지수 백오프 - 비차단 방식 (코루틴 delay 사용)
                        kotlinx.coroutines.delay(100 * (1L shl attempt))
                    }
                } catch (e: Exception) {
                    attempt++
                    logger.error(e) { "WebSocket 메시지 전송 실패: $destination, 메시지 수: ${messages.size}, 시도 횟수: $attempt" }
                    if (attempt < retryCount) {
                        // 지수 백오프 - 비차단 방식 (코루틴 delay 사용)
                        kotlinx.coroutines.delay(100 * (1L shl attempt))
                    }
                }
            }

            if (!success) {
                messagesFailed.addAndGet(messages.size)
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
    }

    /**
     * 애플리케이션 종료 시 리소스 정리
     * 모든 메시지를 처리하고 스케줄러와 코루틴을 정리합니다.
     */
    fun shutdown() {
        try {
            logger.info { "WebSocket 메시지 브로커 종료 시작: 남은 메시지 처리 중..." }

            // 남은 메시지 처리 시도
            try {
                flushMessageBuffer()
            } catch (e: Exception) {
                logger.error(e) { "종료 중 메시지 플러시 실패" }
            }

            // 메트릭 최종 로깅
            logger.info { "WebSocket 메시지 브로커 최종 통계: 성공=${messagesSent.get()}, 실패=${messagesFailed.get()}" }

            // 스케줄러 종료
            logger.info { "WebSocket 메시지 브로커 스케줄러 종료 중..." }
            scheduler.shutdown()
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn { "WebSocket 메시지 브로커 스케줄러 강제 종료" }
                scheduler.shutdownNow()
            }

            logger.info { "WebSocket 메시지 브로커 종료 완료" }
        } catch (e: Exception) {
            logger.error(e) { "WebSocket 메시지 브로커 종료 중 오류 발생" }
            scheduler.shutdownNow()
        }
    }

}
