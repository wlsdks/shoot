package com.stark.shoot.infrastructure.config.kafka

import com.stark.shoot.domain.chat.event.ChatEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConsumerConfig {

    @Value("\${spring.kafka.consumer.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun consumerFactory(): ConsumerFactory<String, ChatEvent> {
        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "shoot-\${random.uuid}", // 인스턴스별 고유 그룹 ID
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "com.stark.shoot.domain.chat.event",
            JsonDeserializer.TYPE_MAPPINGS to "chatEvent:com.stark.shoot.domain.chat.event.ChatEvent",
            JsonDeserializer.VALUE_DEFAULT_TYPE to ChatEvent::class.java.name,
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
        consumerFactory: ConsumerFactory<String, ChatEvent>,
        errorHandler: DefaultErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, ChatEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ChatEvent>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(errorHandler)
        return factory
    }

    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<String, ChatEvent>): DefaultErrorHandler {
        val fixedBackOff = FixedBackOff(1000L, 3) // 3회 재시도, 1초 간격
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            TopicPartition("dead-letter-topic", record.partition())
        }
        return DefaultErrorHandler(recoverer, fixedBackOff)
    }

}