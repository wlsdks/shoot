package com.stark.shoot.adapter.`in`.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * Redis Pub/Sub 메시지를 수신하여 WebSocket으로 브로드캐스트하는 리스너
 */
@Component
class RedisMessageListener(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Redis 메시지 수신 시 호출되는 메서드
     * "chat:room:{roomId}" 채널에서 수신한 메시지를 WebSocket을 통해 클라이언트에 전달합니다.
     */
    fun onMessage(message: String, pattern: String?) {
        try {
            // 채널 이름에서 roomId 추출 (chat:room:123 -> 123)
            val channelParts = pattern?.split(":")
            val roomId = channelParts?.getOrNull(2)

            if (roomId != null) {
                // 메시지 파싱
                val chatMessage = objectMapper.readValue(message, ChatMessageRequest::class.java)

                // WebSocket으로 메시지 즉시 브로드캐스트
                simpMessagingTemplate.convertAndSend("/topic/messages/$roomId", chatMessage)
                logger.debug { "Redis message broadcast to WebSocket: /topic/messages/$roomId" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error processing Redis message: $message" }
        }
    }

}