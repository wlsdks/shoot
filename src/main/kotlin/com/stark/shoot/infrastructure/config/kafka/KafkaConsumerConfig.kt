package com.stark.shoot.infrastructure.config.kafka

import com.stark.shoot.domain.chat.event.ChatEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class KafkaConsumerConfig {

    @Value("\${spring.kafka.producer.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun consumerFactory(): ConsumerFactory<String, ChatEvent> {
        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "shoot",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            // JSON Deserializer 설정
            JsonDeserializer.TRUSTED_PACKAGES to "com.stark.shoot.domain.chat.event",
            JsonDeserializer.TYPE_MAPPINGS to "chatEvent:com.stark.shoot.domain.chat.event.ChatEvent",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest"
        )

        return DefaultKafkaConsumerFactory(
            configProps,
            StringDeserializer(),
            JsonDeserializer(ChatEvent::class.java, false)
        )
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, ChatEvent>
    ): ConcurrentKafkaListenerContainerFactory<String, ChatEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ChatEvent>()
        factory.consumerFactory = consumerFactory
        return factory
    }

}