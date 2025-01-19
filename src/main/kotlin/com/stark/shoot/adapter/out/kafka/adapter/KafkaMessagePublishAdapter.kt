package com.stark.shoot.adapter.out.kafka.adapter

import com.stark.shoot.application.port.out.kafka.KafkaMessagePublishPort
import com.stark.shoot.domain.chat.event.ChatEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class KafkaMessagePublishAdapter(
    private val kafkaTemplate: KafkaTemplate<String, ChatEvent>
) : KafkaMessagePublishPort {

    private val logger = KotlinLogging.logger {}

    /**
     * 채팅 이벤트를 Kafka로 발행합니다.
     */
    override fun publishChatEvent(
        topic: String,
        key: String,
        event: ChatEvent
    ): CompletableFuture<Void> {
        return kafkaTemplate.send(topic, key, event)
            // 비동기로 메시지 전송 후 로깅
            .thenAccept { result ->
                logger.info { "Message sent to topic: $topic, key: $key, event: $event" }
            }
            // 에러 처리
            .exceptionally { ex ->
                logger.error(ex) { "Failed to send message to topic: $topic, key: $key, event: $event" }
                throw RuntimeException("Failed to publish message", ex)
            }
    }

}