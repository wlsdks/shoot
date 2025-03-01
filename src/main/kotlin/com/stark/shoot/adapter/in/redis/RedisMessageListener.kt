package com.stark.shoot.adapter.`in`.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class RedisMessageListener(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = KotlinLogging.logger {}

    // Redis 메시지 수신 시 호출되는 메서드
    // Redis 서버에서 "chat:room:*" 패턴과 일치하는 채널에 메시지가 발행되면, Spring Data Redis가 자동으로 이 리스너의 onMessage 메서드를 호출합니다.
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