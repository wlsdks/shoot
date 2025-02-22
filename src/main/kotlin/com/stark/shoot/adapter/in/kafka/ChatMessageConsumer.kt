package com.stark.shoot.adapter.`in`.kafka

import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class ChatMessageConsumer(
    private val processMessageUseCase: ProcessMessageUseCase,
    private val redisTemplate: RedisTemplate<String, ChatMessage>,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = KotlinLogging.logger {}

    @KafkaListener(topics = ["chat-messages"], groupId = "shoot-\${random.uuid}")
    fun consumeMessage(@Payload event: ChatEvent) {
        if (event.type == EventType.MESSAGE_CREATED) {
            // ChatMessage 객체 처리
            val savedMessage = processMessageUseCase.processMessage(event.data)

            // WebSocket으로 ChatMessage 객체 전송
            messagingTemplate.convertAndSend("/topic/messages/${savedMessage.roomId}", savedMessage)
            logger.info { "Message pushed to /topic/messages/${savedMessage.roomId}: ${savedMessage.content.text}" }

            // Redis에 ChatMessage 객체 저장 (ChatMessage 객체를 chat:<roomId>:messages 키에 List로 저장, 최근 10개만 유지.)
            // 역할: 클라이언트가 채팅방에 입장할 때 최근 메시지를 빠르게 로드하기 위함. MongoDB 조회를 줄여 성능 향상.
            redisTemplate.opsForList().leftPush("chat:${savedMessage.roomId}:messages", savedMessage)
            redisTemplate.opsForList().trim("chat:${savedMessage.roomId}:messages", 0, 9)
        }
    }

}