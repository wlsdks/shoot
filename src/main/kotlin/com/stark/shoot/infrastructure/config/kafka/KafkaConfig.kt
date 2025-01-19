package com.stark.shoot.infrastructure.config.kafka

import com.stark.shoot.domain.chat.event.ChatEvent
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaConfig {

    @Value("\${spring.kafka.producer.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun kafkaTemplate(
        producerFactory: ProducerFactory<String, ChatEvent>
    ): KafkaTemplate<String, ChatEvent> {
        return KafkaTemplate(producerFactory)
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, ChatEvent> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            // 추가 설정
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.BATCH_SIZE_CONFIG to 16384,
            ProducerConfig.LINGER_MS_CONFIG to 1
        )

        return DefaultKafkaProducerFactory(configProps)
    }
}

// 토픽 상수는 별도 object 파일로 분리
object KafkaTopics {
    const val CHAT_MESSAGES = "chat-messages"
    const val CHAT_NOTIFICATIONS = "chat-notifications"
    const val CHAT_EVENTS = "chat-events"
}