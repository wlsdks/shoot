package com.stark.shoot.adapter.`in`.socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageStatusResponse
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
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

    // 새로 추가하는 메서드
    fun sendToUser(
        userId: String,
        destination: String,
        payload: Any,
        retryCount: Int = 3
    ): CompletableFuture<Boolean> {
        val result = CompletableFuture<Boolean>()

        coroutineScope.launch {
            var attempt = 0
            var success = false

            while (!success && attempt < retryCount) {
                try {
                    withTimeout(MESSAGE_SEND_TIMEOUT) {
                        simpMessagingTemplate.convertAndSendToUser(userId, destination, payload)
                        logger.debug {
                            "사용자에게 메시지 전송 성공: userId=$userId, destination=$destination, attempt=${attempt + 1}"
                        }
                    }
                    success = true
                } catch (e: TimeoutCancellationException) {
                    attempt++
                    logger.warn {
                        "사용자 메시지 전송 시간 초과: userId=$userId, destination=$destination, attempt=$attempt/$retryCount"
                    }
                    if (attempt < retryCount) {
                        delay(1000L * attempt) // 지수 백오프
                    }
                } catch (e: Exception) {
                    attempt++
                    logger.error(e) {
                        "사용자 메시지 전송 실패: userId=$userId, destination=$destination, attempt=$attempt/$retryCount"
                    }
                    if (attempt < retryCount) {
                        delay(1000L * attempt) // 지수 백오프
                    }
                }
            }

            if (success) {
                result.complete(true)
                logger.info { "사용자에게 메시지 전송 완료: userId=$userId, destination=$destination" }
            } else {
                result.complete(false)
                logger.error {
                    "사용자 메시지 전송 최종 실패: userId=$userId, destination=$destination, 총 시도 횟수=$attempt"
                }
            }
        }

        return result
    }

    fun shutdown() {
        logger.info { "WebSocket 메시지 브로커 종료" }
    }

}
