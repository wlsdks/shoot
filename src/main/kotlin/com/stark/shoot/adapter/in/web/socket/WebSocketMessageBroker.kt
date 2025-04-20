package com.stark.shoot.adapter.`in`.web.socket

import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class WebSocketMessageBroker(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val redisTemplate: StringRedisTemplate
) {

    private val logger = KotlinLogging.logger {}

    /**
     * WebSocket을 통해 메시지를 전송합니다.
     * - Redis에 실패한 메시지를 저장합니다.
     *
     * @param destination 전송할 WebSocket의 목적지
     * @param payload 전송할 메시지
     * @param retryCount 재시도 횟수 (기본값: 3)
     */
    fun sendMessage(destination: String, payload: Any, retryCount: Int = 3) {
        var attempt = 0
        var success = false

        while (!success && attempt < retryCount) {
            try {
                simpMessagingTemplate.convertAndSend(destination, payload)
                success = true
                // 전송 성공 로그
            } catch (e: Exception) {
                attempt++
                logger.error(e) { "WebSocket 메시지 전송 실패: $destination, 시도 횟수: $attempt" }
                if (attempt < retryCount) {
                    Thread.sleep(100 * (1L shl attempt)) // 지수 백오프
                }
            }
        }

        if (!success) {
            // 최대 재시도 횟수 초과 시 Redis에 메시지 저장
            if (payload is MessageStatusResponse) {
                val tempId = payload.tempId
                redisTemplate.opsForValue().set("failed-message-tempId:$tempId", payload.toString())
            } else {
                redisTemplate.opsForValue().set("failed-message:${System.currentTimeMillis()}", payload.toString())
            }
            logger.error { "WebSocket 메시지 전송 실패: $destination, 최대 재시도 횟수 초과" }
        }
    }

}