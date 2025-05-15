package com.stark.shoot.adapter.out.kafka.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.notification.PublishNotificationEventPort
import com.stark.shoot.domain.notification.event.NotificationEvent
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate

@Adapter
class PublishNotificationEventKafkaAdapter(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : PublishNotificationEventPort {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val NOTIFICATION_EVENTS_TOPIC = "notification-events"
    }

    /**
     * 알림 이벤트를 Kafka에 발행합니다.
     *
     * @param event 알림 이벤트 객체
     * @return 성공 여부
     */
    override fun publishEvent(event: NotificationEvent): Boolean {
        try {
            // JSON 문자열로 변환
            val eventJson = objectMapper.writeValueAsString(event)
            logger.debug { "Publishing notification event: $eventJson" }

            // Kafka에 알림 이벤트 발행
            val future = kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, event.sourceId, eventJson)

            future.whenComplete { result, ex ->
                if (ex != null) {
                    logger.error(ex) { "Failed to publish notification event: ${event.id}" }
                } else {
                    logger.debug { "Successfully published notification event: ${event.id}, offset: ${result.recordMetadata.offset()}" }
                }
            }

            return true
        } catch (e: Exception) {
            logger.error(e) { "Error publishing notification event: ${event.id}" }
            return false
        }
    }

}
