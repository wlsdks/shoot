package com.stark.shoot.adapter.out.kafka

import com.stark.shoot.application.port.out.kafka.PublishKafkaPort
import com.stark.shoot.domain.shared.event.MessageEvent
import com.stark.shoot.infrastructure.exception.web.KafkaPublishException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class PublishKafkaAdapter(
    private val kafkaTemplate: KafkaTemplate<String, MessageEvent>
) : PublishKafkaPort {

    private val logger = KotlinLogging.logger {}

    /**
     * 채팅 작성 메시지 발행
     */
    override fun publishChatEvent(
        topic: String,
        key: String,
        event: MessageEvent
    ): CompletableFuture<Void> {
        return kafkaTemplate.send(topic, key, event)
            .thenAccept { result ->
//                logger.info { "Message sent to topic: $topic, key: $key, offset: ${result.recordMetadata.offset()}" }
            }
            .exceptionally { ex ->
                logger.error(ex) { "Failed to send message to topic: $topic, key: $key, event: $event" }
                throw KafkaPublishException("Failed to publish message to Kafka", ex)
            }
    }

    /**
     * 채팅 작성 메시지 발행 (코루틴 기반)
     */
    override suspend fun publishChatEventSuspend(
        topic: String,
        key: String,
        event: MessageEvent
    ) {
        try {
            val result = kafkaTemplate.send(topic, key, event).await()
//            logger.info { "Message sent to topic: $topic, key: $key, offset: ${result.recordMetadata.offset()}" }
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to send message to topic: $topic, key: $key, event: $event" }
            throw KafkaPublishException("Failed to publish message to Kafka", ex)
        }
    }

}