package com.stark.shoot.adapter.out.kafka.adapter

import com.stark.shoot.application.port.out.kafka.KafkaMessagePublishPort
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.infrastructure.exception.web.KafkaPublishException
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
            .thenAccept { result ->
                logger.info { "Message sent to topic: $topic, key: $key, offset: ${result.recordMetadata.offset()}" }
            }
            .exceptionally { ex ->
                logger.error(ex) { "Failed to send message to topic: $topic, key: $key, event: $event" }
                throw KafkaPublishException("Failed to publish message to Kafka", ex)
            }
    }

}