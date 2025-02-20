package com.stark.shoot.infrastructure.config.kafka

import com.stark.shoot.domain.chat.event.ChatEvent
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.KafkaListenerErrorHandler

@Configuration
class KafkaProducerConfig {

    @Value("\${spring.kafka.producer.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    // schema.registry.url을 프로퍼티나 기본값으로 설정
    @Value("\${schema.registry.url:http://localhost:8111}")
    private lateinit var schemaRegistryUrl: String

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
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            "schema.registry.url" to schemaRegistryUrl,
            // 메시지 순서 보장을 위한 설정
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 5,
            // 신뢰성 관련 설정 (acks=all로 모든 복제본에 저장 확인)
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            // 성능 관련 설정
            ProducerConfig.BATCH_SIZE_CONFIG to 16384,
            ProducerConfig.LINGER_MS_CONFIG to 1,
            // 메모리 버퍼 설정 (프로듀서가 사용할 메모리 버퍼 크기)
            ProducerConfig.BUFFER_MEMORY_CONFIG to 33554432,
            // 압축 설정 (메시지 압축 방식 설정)
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy"
        )

        return DefaultKafkaProducerFactory(configProps)
    }

    // 에러 핸들러 빈 등록
    @Bean
    fun chatMessageErrorHandler(): KafkaListenerErrorHandler {
        return KafkaListenerErrorHandler { _, exception ->
            println("Error occurred: $exception")
        }
    }

}
