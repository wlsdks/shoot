package com.stark.shoot.infrastructure.config.kafka

import com.stark.shoot.domain.chat.event.ChatEvent
import io.confluent.kafka.serializers.KafkaAvroDeserializer
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
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConsumerConfig {

    @Value("\${spring.kafka.producer.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    // schema.registry.url을 프로퍼티나 기본값으로 설정
    @Value("\${schema.registry.url:http://localhost:8111}")
    private lateinit var schemaRegistryUrl: String

    @Bean
    fun consumerFactory(): ConsumerFactory<String, ChatEvent> {
        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "shoot",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,

            // Avro 스키마 레지스트리 설정
            "schema.registry.url" to schemaRegistryUrl,       // 스키마 레지스트리 URL
            "specific.avro.reader" to true,                   // Avro SpecificRecord 사용
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
        )

        // KafkaAvroDeserializer를 ChatEvent 타입으로 캐스팅하여 사용
        @Suppress("UNCHECKED_CAST")
        val avroDeserializer = KafkaAvroDeserializer().apply {
            configure(configProps, false)
        } as org.apache.kafka.common.serialization.Deserializer<ChatEvent>

        // 제네릭 타입을 명시적으로 지정하여 타입 불일치 해결
        return DefaultKafkaConsumerFactory(
            configProps,
            StringDeserializer(),
            avroDeserializer
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
    fun kafkaErrorHandler(
        kafkaTemplate: KafkaTemplate<String, ChatEvent>
    ): DefaultErrorHandler {
        // 재시도: 3회, 1초 간격
        val fixedBackOff = FixedBackOff(1000L, 3)

        // BiFunction<ConsumerRecord<*, *>, Exception, TopicPartition>를 람다로 작성
        val recoverer = DeadLetterPublishingRecoverer(
            kafkaTemplate
        ) { record, _ ->
            // DLT 토픽 이름과 partition 지정 (여기서는 record의 partition 번호 사용)
            TopicPartition("dead-letter-topic", record.partition())
        }

        return DefaultErrorHandler(recoverer, fixedBackOff)
    }

}