package com.stark.shoot.infrastructure.config.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.out.kafka.PublishNotificationKafkaAdapter
import com.stark.shoot.adapter.out.redis.notification.SendNotificationRedisAdapter
import com.stark.shoot.adapter.out.socket.notification.SendNotificationWebSocketAdapter
import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.kafka.core.KafkaTemplate

/**
 * 알림 전송 관련 설정 클래스
 * Redis 또는 Kafka를 사용하여 알림을 전송하는 구현체를 선택할 수 있습니다.
 */
@Configuration
class NotificationConfig {

    private val logger = KotlinLogging.logger {}

    /**
     * 알림 전송 방식을 선택합니다.
     * application.yml에서 notification.transport 속성으로 설정할 수 있습니다.
     * 기본값은 "redis"입니다.
     *
     * @param notificationTransport 알림 전송 방식 ("redis" 또는 "kafka")
     * @param redisTemplate Redis 템플릿
     * @param kafkaTemplate Kafka 템플릿
     * @param objectMapper JSON 직렬화/역직렬화를 위한 ObjectMapper
     * @return 선택된 SendNotificationPort 구현체
     */
    @Bean
    @Primary
    fun sendNotificationPort(
        @Value("\${notification.transport:redis}") notificationTransport: String,
        redisTemplate: StringRedisTemplate,
        kafkaTemplate: KafkaTemplate<String, String>,
        webSocketMessageBroker: WebSocketMessageBroker,
        objectMapper: ObjectMapper
    ): SendNotificationPort {
        logger.info { "알림 전송 방식: $notificationTransport" }
        
        return when (notificationTransport.lowercase()) {
            "kafka" -> {
                logger.info { "Kafka를 사용하여 알림을 전송합니다." }
                PublishNotificationKafkaAdapter(kafkaTemplate, objectMapper)
            }
            "websocket" -> {
                logger.info { "WebSocket을 사용하여 알림을 전송합니다." }
                SendNotificationWebSocketAdapter(webSocketMessageBroker)
            }
            else -> {
                logger.info { "Redis를 사용하여 알림을 전송합니다." }
                SendNotificationRedisAdapter(redisTemplate, objectMapper)
            }
        }
    }

    /**
     * Redis를 사용하는 알림 전송 구현체를 제공합니다.
     * 이 빈은 "redisNotificationSender"라는 이름으로 등록됩니다.
     *
     * @param redisTemplate Redis 템플릿
     * @param objectMapper JSON 직렬화/역직렬화를 위한 ObjectMapper
     * @return Redis 기반 SendNotificationPort 구현체
     */
    @Bean("redisNotificationSender")
    fun redisNotificationSender(
        redisTemplate: StringRedisTemplate,
        objectMapper: ObjectMapper
    ): SendNotificationPort {
        return SendNotificationRedisAdapter(redisTemplate, objectMapper)
    }

    /**
     * Kafka를 사용하는 알림 전송 구현체를 제공합니다.
     * 이 빈은 "kafkaNotificationSender"라는 이름으로 등록됩니다.
     *
     * @param kafkaTemplate Kafka 템플릿
     * @param objectMapper JSON 직렬화/역직렬화를 위한 ObjectMapper
     * @return Kafka 기반 SendNotificationPort 구현체
     */
    @Bean("kafkaNotificationSender")
    fun kafkaNotificationSender(
        kafkaTemplate: KafkaTemplate<String, String>,
        objectMapper: ObjectMapper
    ): SendNotificationPort {
        return PublishNotificationKafkaAdapter(kafkaTemplate, objectMapper)
    }

    @Bean("websocketNotificationSender")
    fun websocketNotificationSender(
        webSocketMessageBroker: WebSocketMessageBroker
    ): SendNotificationPort {
        return SendNotificationWebSocketAdapter(webSocketMessageBroker)
    }
}