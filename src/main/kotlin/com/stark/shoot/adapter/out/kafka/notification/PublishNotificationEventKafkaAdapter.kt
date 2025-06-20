package com.stark.shoot.adapter.out.kafka.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.notification.PublishNotificationEventPort
import com.stark.shoot.domain.event.NotificationEvent
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.KafkaPublishException
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
     * @throws KafkaPublishException 이벤트 발행 실패 시 발생
     */
    override fun publishEvent(event: NotificationEvent) {
        try {
            // JSON 문자열로 변환
            val eventJson = objectMapper.writeValueAsString(event)

            // Kafka에 알림 이벤트 발행
            val future = kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, event.sourceId, eventJson)

            future.whenComplete { result, ex ->
                if (ex != null) {
                    throw KafkaPublishException("Failed to publish notification event: ${ex.message}", ex)
                }
            }
        } catch (e: Exception) {
            throw KafkaPublishException("Failed to publish notification event: ${e.message}", e)
        }
    }

}
