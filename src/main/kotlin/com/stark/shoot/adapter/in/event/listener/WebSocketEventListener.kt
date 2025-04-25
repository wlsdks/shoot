package com.stark.shoot.adapter.`in`.event.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class WebSocketEventListener(
    private val redisTemplate: StringRedisTemplate
) {
    private val logger = KotlinLogging.logger {}

    // 연결 통계를 위한 카운터
    private val activeConnections = AtomicInteger(0)
    private val connectionsByUser = ConcurrentHashMap<String, AtomicInteger>()

    // 마지막 로그 시간 (초당 로그 제한용)
    private var lastLogTime = System.currentTimeMillis()

    /**
     * WebSocket 연결 이벤트 처리
     * - 사용자 연결 상태를 Redis에 기록
     * - 연결 통계 업데이트
     */
    @EventListener
    fun handleConnect(event: SessionConnectEvent) {
        val userId = event.user?.name ?: run {
            logger.debug { "No userId found in connect event: ${event.message.headers}" }
            return
        }

        // 연결 통계 업데이트
        activeConnections.incrementAndGet()
        connectionsByUser.computeIfAbsent(userId) { AtomicInteger(0) }.incrementAndGet()

        // 로깅 제한 (1초에 한 번만)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLogTime > 1000) {
            lastLogTime = currentTime
            logger.info { "Active WebSocket connections: ${activeConnections.get()} (users: ${connectionsByUser.size})" }
        }
    }

    /**
     * WebSocket 연결 종료 이벤트 처리
     * - 사용자 연결 상태를 Redis에 기록 (파이프라인 사용)
     * - 연결 통계 업데이트
     */
    @EventListener
    fun handleDisconnect(event: SessionDisconnectEvent) {
        // WebSocket 연결 시 userId 설정 필요
        val userId = event.user?.name ?: run {
            logger.warn { "No userId found in disconnect event: ${event.sessionId}" }
            return
        }

        // 연결 통계 업데이트
        activeConnections.decrementAndGet()
        connectionsByUser[userId]?.decrementAndGet()?.let { count ->
            if (count <= 0) {
                connectionsByUser.remove(userId)
            }
        }

        logger.info { "User disconnected: $userId (reason: ${event.closeStatus?.reason ?: "unknown"})" }

        // Redis 파이프라인을 사용하여 모든 active key를 한 번에 false로 설정
        try {
            val activeKeys = redisTemplate.keys("active:$userId:*")
            if (activeKeys.isNotEmpty()) {
                redisTemplate.executePipelined { connection: RedisConnection ->
                    for (key in activeKeys) {
                        connection.stringCommands().set(
                            key.toByteArray(),
                            "false".toByteArray()
                        )
                    }
                    null
                }
                logger.debug { "Set ${activeKeys.size} active keys to false for user: $userId" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to update Redis active keys for user: $userId" }
        }
    }
}
