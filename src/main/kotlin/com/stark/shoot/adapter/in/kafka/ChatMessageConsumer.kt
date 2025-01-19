package com.stark.shoot.adapter.`in`.kafka

import com.stark.shoot.application.port.`in`.chat.ProcessMessageUseCase
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ChatMessageConsumer(
    private val processMessageUseCase: ProcessMessageUseCase,
    private val redisTemplate: StringRedisTemplate
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 채팅 메시지 이벤트를 수신하여 처리합니다.
     */
    @KafkaListener(topics = ["chat-messages"])
    fun consumeMessage(event: ChatEvent) {
        when (event.type) {
            EventType.MESSAGE_CREATED -> {
                try {
                    // 도메인 서비스를 통한 메시지 처리
                    val processedMessage = processMessageUseCase.processMessage(event.data)

                    // Redis 캐시 업데이트
                    updateRedisCache(processedMessage)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to process message: ${event.data}" }
                    // 에러 처리 로직 (재시도 등)
                }
            }
            // 다른 이벤트 타입 처리
            else -> {}
        }
    }

    // Redis 캐시 업데이트
    private fun updateRedisCache(message: ChatMessage) {
        updateUnreadCount(message.roomId, message.senderId)
        updateRecentMessages(message)
    }

    // 읽지 않은 메시지 카운트 업데이트
    private fun updateUnreadCount(roomId: String, senderId: String) {
        val unreadKey = "chat:unread:$roomId"
        redisTemplate.opsForHash<String, String>()
            .increment(unreadKey, senderId, 1)
    }

    // 최근 메시지 목록 업데이트
    private fun updateRecentMessages(message: ChatMessage) {
        val recentKey = "chat:recent:${message.roomId}"
        redisTemplate.opsForZSet()
            .add(
                recentKey,
                message.id ?: throw IllegalArgumentException("Message ID is required"),
                message.createdAt.toEpochMilli().toDouble()
            )
    }

}