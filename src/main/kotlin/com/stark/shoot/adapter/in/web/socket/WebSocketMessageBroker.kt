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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Component
class WebSocketMessageBroker(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val coroutineScope: ApplicationCoroutineScope
) {
    private val logger = KotlinLogging.logger {}
    private val MESSAGE_SEND_TIMEOUT = 5000L

    @PreDestroy
    fun onApplicationShutdown() {
        logger.info { "애플리케이션 종료: WebSocket 메시지 브로커 리소스 정리 중..." }
        shutdown()
    }

    /**
     * WebSocket을 통해 메시지를 전송합니다.
     * 실패 시 재시도 후 Redis에 저장합니다.
     */
    fun sendMessage(destination: String, payload: Any, retryCount: Int = 3): CompletableFuture<Boolean> {
        val result = CompletableFuture<Boolean>()

        coroutineScope.launch {
            var attempt = 0
            var success = false

            while (!success && attempt < retryCount) {
                try {
                    withTimeout(MESSAGE_SEND_TIMEOUT) {
                        simpMessagingTemplate.convertAndSend(destination, payload)
                    }
                    success = true
                } catch (e: Exception) {
                    attempt++
                    val errorMessage = when (e) {
                        is TimeoutCancellationException -> "WebSocket 메시지 전송 타임아웃"
                        else -> "WebSocket 메시지 전송 실패"
                    }
                    logger.error(e) { "$errorMessage: $destination, 시도 횟수: $attempt" }
                }
            }

            if (!success) {
                try {
                    val key = when (payload) {
                        is MessageStatusResponse -> "failed-message-tempId:${payload.tempId}"
                        else -> "failed-message:${System.currentTimeMillis()}"
                    }
                    val value = objectMapper.writeValueAsString(payload)
                    redisTemplate.opsForValue().set(key, value, 24, TimeUnit.HOURS)
                } catch (e: Exception) {
                    logger.error(e) { "Redis에 실패한 메시지 저장 중 오류 발생" }
                }
            }

            result.complete(success)
        }.invokeOnCompletion { throwable ->
            throwable?.let {
                logger.error(it) { "코루틴 처리중 예외 발생" }
                if (!result.isDone) result.complete(false)
            }
        }

        return result
    }

    fun shutdown() {
        logger.info { "WebSocket 메시지 브로커 종료" }
    }

}
