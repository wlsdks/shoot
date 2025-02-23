package com.stark.shoot.adapter.`in`.event.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketEventListener(
    private val redisTemplate: StringRedisTemplate
) {

    private val logger = KotlinLogging.logger {}

    // event.user?.name이 제대로 설정돼 있어야 한다.
    // AuthHandshakeInterceptor에서 WebSocket 연결 시 Principal에 userId를 넣는지 확인해야 한다.
    @EventListener
    fun handleDisconnect(event: SessionDisconnectEvent) {
        // WebSocket 연결 시 userId 설정 필요
        val userId = event.user?.name ?: run {
            logger.warn { "No userId found in disconnect event: ${event.sessionId}" }
            return
        }

        logger.info { "User disconnected: $userId" }
        val activeKeys = redisTemplate.keys("active:$userId:*")

        // 연결 종료 시 모든 active key를 false로 설정
        activeKeys.forEach { key ->
            redisTemplate.opsForValue().set(key, "false")
            logger.info { "WebSocket disconnected, set $key to false" }
        }
    }

}