package com.stark.shoot.adapter.out.kafka.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.notification.PublishNotificationEventPort
import com.stark.shoot.domain.notification.event.NotificationEvent
import com.stark.shoot.infrastructure.annotation.Adapter
import org.springframework.kafka.core.KafkaTemplate

@Adapter
class PublishNotificationEventKafkaAdapter(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : PublishNotificationEventPort {

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
            // Convert the event to a JSON string
            val eventJson = objectMapper.writeValueAsString(event)

            // Publish the event to the Kafka topic
            kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, event.sourceId, eventJson)

            return true
        } catch (e: Exception) {
            // Log the error
            println("Error publishing notification event: ${e.message}")
            return false
        }
    }

}