package com.stark.shoot.adapter.`in`.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * Redis Pub/Sub 메시지를 수신하여 WebSocket으로 브로드캐스트하는 리스너
 */
@Deprecated("Redis Stream을 사용하도록 변경")
@Component
class RedisMessageListener(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper
) : MessageListener {

    private val logger = KotlinLogging.logger {}

    companion object {
        private val ROOM_ID_PATTERN = Regex("chat:room:([^:]+)")
    }

    /**
     * Redis 메시지 수신 시 호출되는 메서드
     * "chat:room:{roomId}" 채널에서 수신한 메시지를 WebSocket을 통해 클라이언트에 전달합니다.
     */
    override fun onMessage(message: Message, pattern: ByteArray?) {
        try {
            // 메시지 바디와 채널 정보를 문자열로 변환
            val messageBody = String(message.body, StandardCharsets.UTF_8)
            val channel = String(message.channel, StandardCharsets.UTF_8)
            logger.debug { "Received Redis message body: $messageBody, channel: $channel" }

            // 채널에서 roomId 추출
            val roomIdMatch = ROOM_ID_PATTERN.find(channel)
            val roomId = roomIdMatch?.groupValues?.getOrNull(1)

            if (roomId != null) {
                val chatMessage = objectMapper.readValue(messageBody, ChatMessageRequest::class.java)
                simpMessagingTemplate.convertAndSend("/topic/messages/$roomId", chatMessage)
            } else {
                logger.warn { "Could not extract roomId from channel: $channel" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Redis 메시지 처리 오류: ${String(message.body, StandardCharsets.UTF_8)}" }
            throw e // 예외를 전파하여 상위에서 처리 가능하도록
        }
    }

}