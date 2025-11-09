package com.stark.shoot.adapter.out.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.DeadLetterMessage
import com.stark.shoot.application.port.out.DeadLetterQueuePort
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

/**
 * Kafka를 통한 Dead Letter Queue 발행 Adapter
 *
 * application.yml에서 설정:
 * ```yaml
 * kafka:
 *   topics:
 *     dead-letter-queue: dead-letter-queue
 * ```
 */
@Adapter
@ConditionalOnProperty(
    prefix = "kafka.topics",
    name = ["dead-letter-queue"],
    matchIfMissing = false
)
class DeadLetterQueueKafkaAdapter(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @Value("\${kafka.topics.dead-letter-queue:dead-letter-queue}")
    private val dlqTopic: String
) : DeadLetterQueuePort {

    private val logger = KotlinLogging.logger {}

    override fun publish(message: DeadLetterMessage) {
        try {
            val messageJson = objectMapper.writeValueAsString(message)

            val future: CompletableFuture<SendResult<String, String>> = kafkaTemplate.send(
                dlqTopic,
                message.sagaId,  // 파티션 키: sagaId로 순서 보장
                messageJson
            )

            future.whenComplete { result, ex ->
                if (ex == null) {
                    logger.info {
                        "DLQ message published: sagaId=${message.sagaId}, " +
                                "sagaType=${message.sagaType}, " +
                                "partition=${result.recordMetadata.partition()}"
                    }
                } else {
                    logger.error(ex) {
                        "Failed to publish DLQ message: sagaId=${message.sagaId}, " +
                                "sagaType=${message.sagaType}"
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Exception while publishing DLQ message: sagaId=${message.sagaId}" }
        }
    }
}

/**
 * Kafka DLQ가 비활성화되어 있을 때 사용하는 No-op Adapter
 * 로그만 출력합니다.
 */
@Adapter
@ConditionalOnProperty(
    prefix = "kafka.topics",
    name = ["dead-letter-queue"],
    matchIfMissing = true,
    havingValue = ""
)
class NoOpDeadLetterQueueAdapter : DeadLetterQueuePort {
    private val logger = KotlinLogging.logger {}

    override fun publish(message: DeadLetterMessage) {
        logger.warn {
            "DLQ disabled - Message would be published: \n" +
                    "SagaId: ${message.sagaId}\n" +
                    "SagaType: ${message.sagaType}\n" +
                    "Failed Steps: ${message.failedSteps}\n" +
                    "Error: ${message.errorDetails}\n" +
                    "Payload: ${message.payload.take(200)}..." // 처음 200자만
        }
    }
}
