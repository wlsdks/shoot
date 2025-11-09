package com.stark.shoot.adapter.out.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.KafkaPublishException
import com.stark.shoot.infrastructure.exception.web.RedisOperationException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate

@Adapter
class PublishNotificationKafkaAdapter(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : SendNotificationPort {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val NOTIFICATION_TOPIC = "user-notifications"
    }

    /**
     * 알림을 Kafka를 통해 전송합니다.
     * 사용자별 파티션 키를 사용하여 알림을 발행합니다.
     *
     * @param notification 알림 객체
     * @throws RedisOperationException 알림 전송 실패 시 발생
     */
    override fun sendNotification(notification: Notification) {
        try {
            // 사용자 ID를 파티션 키로 사용하여 같은 사용자의 알림이 순서대로 처리되도록 함
            val key = notification.userId.value.toString()
            val notificationJson = objectMapper.writeValueAsString(notification)

            // Kafka 토픽에 알림 발행
            val future = kafkaTemplate.send(NOTIFICATION_TOPIC, key, notificationJson)

            future.whenComplete { result, ex ->
                if (ex != null) {
                    logger.error(ex) { "Kafka를 통한 알림 전송 중 오류가 발생했습니다: ${ex.message}" }
                    throw KafkaPublishException("Kafka를 통한 알림 전송 중 오류가 발생했습니다: ${ex.message}", ex)
                } else {
                    logger.info { "알림이 Kafka 토픽에 발행되었습니다: userId=${notification.userId.value}, type=${notification.type}, offset=${result.recordMetadata.offset()}" }
                }
            }
        } catch (e: Exception) {
            val errorMessage = "Kafka를 통한 알림 전송 중 오류가 발생했습니다: ${e.message}"
            logger.error(e) { errorMessage }
            // SendNotificationPort 인터페이스에 정의된 예외를 던짐
            throw RedisOperationException(errorMessage, e)
        }
    }

    /**
     * 여러 알림을 Kafka를 통해 전송합니다.
     * 각 알림은 사용자별 파티션 키를 사용하여 발행됩니다.
     *
     * @param notifications 알림 객체 목록
     * @throws RedisOperationException 알림 전송 실패 시 발생
     */
    override fun sendNotifications(notifications: List<Notification>) {
        if (notifications.isEmpty()) {
            logger.info { "전송할 알림이 없습니다." }
            return
        }

        try {
            // 각 알림을 개별적으로 발행
            notifications.forEach { notification ->
                sendNotification(notification)
            }

            logger.info { "${notifications.size}개의 알림이 Kafka 토픽에 발행되었습니다." }
        } catch (e: Exception) {
            val errorMessage = "Kafka를 통한 다중 알림 전송 중 오류가 발생했습니다: ${e.message}"
            logger.error(e) { errorMessage }
            throw RedisOperationException(errorMessage, e)
        }
    }

}