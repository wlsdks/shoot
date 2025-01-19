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
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaProducerConfig {

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
            // 메시지 순서 보장을 위한 설정
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,                  // 메시지 중복 전송 방지
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 5,         // 동시에 처리할 수 있는 요청 수 제한
            // 신뢰성 관련 설정 (acks=all로 모든 복제본에 저장 확인)
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            // 성능 관련 설정
            ProducerConfig.BATCH_SIZE_CONFIG to 16384,
            ProducerConfig.LINGER_MS_CONFIG to 1,
            // 메모리 버퍼 설정 (프로듀서가 사용할 메모리 버퍼 크기)
            ProducerConfig.BUFFER_MEMORY_CONFIG to 33554432, // 32MB
            // 압축 설정 (메시지 압축 방식 설정)
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy"
        )

        return DefaultKafkaProducerFactory(configProps)
    }

    // 에러 핸들러 빈 등록
    @Bean
    fun chatMessageErrorHandler(): KafkaListenerErrorHandler {
        return KafkaListenerErrorHandler { _, exception ->
            // 에러 처리 로직
            println("Error occurred: $exception")
        }
    }

}