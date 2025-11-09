package com.stark.shoot.infrastructure.config.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.domain.shared.event.MessageEvent
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
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff
import java.net.InetAddress

@Configuration
class KafkaConsumerConfig(
    private val objectMapper: ObjectMapper
) {

    @Value("\${spring.kafka.consumer.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id}")
    private lateinit var groupId: String

    // 컨슈머 동시성 설정 (기본값 3)
    @Value("\${spring.kafka.consumer.concurrency:1}")
    private var concurrency: Int = 1

    @Bean
    fun consumerFactory(): ConsumerFactory<String, MessageEvent> {
        // 호스트명을 가져와 인스턴스별 고유 그룹 ID 생성
        val hostname = try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "unknown-host"
        }

        val configProps = mapOf(
            // 기본 설정
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,

            // 내부 디시리얼라이저 설정
            ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS to StringDeserializer::class.java.name,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to JsonDeserializer::class.java.name,

            // 디시리얼라이저 설정
            JsonDeserializer.TRUSTED_PACKAGES to "com.stark.shoot.domain.event",
            JsonDeserializer.TYPE_MAPPINGS to "chatEvent:com.stark.shoot.domain.event.MessageEvent",
            JsonDeserializer.VALUE_DEFAULT_TYPE to MessageEvent::class.java.name,

            // 오프셋 설정
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,

            // 성능 관련 설정
            ConsumerConfig.FETCH_MIN_BYTES_CONFIG to 1024, // 최소 1KB 데이터를 모아서 가져옴
            ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to 500, // 최대 500ms 대기
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 500, // 한 번에 최대 500개 레코드 처리

            // 세션 타임아웃 설정
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to 30000, // 30초
            ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG to 10000 // 10초
        )

        // 내부 디시리얼라이저 생성
        val keyDeserializer = ErrorHandlingDeserializer(StringDeserializer())

        // 커스텀 ObjectMapper를 사용하는 JsonDeserializer 생성
        val jsonDeserializer = JsonDeserializer(MessageEvent::class.java, objectMapper, false)

        val valueDeserializer = ErrorHandlingDeserializer(jsonDeserializer)

        return DefaultKafkaConsumerFactory(
            configProps,
            keyDeserializer,
            valueDeserializer
        )
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, MessageEvent>,
        errorHandler: DefaultErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, MessageEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, MessageEvent>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(errorHandler)

        // 중요: AckMode를 명시적으로 MANUAL로 설정
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL

        // 동시성 설정 (여러 스레드로 메시지 처리)
        factory.setConcurrency(concurrency)

        // 배치 리스너 설정
        factory.isBatchListener = false

        // 자동 시작 설정
        factory.setAutoStartup(true)

        return factory
    }

    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<String, MessageEvent>): DefaultErrorHandler {
        // 재시도 설정: 1초 간격으로 3회 재시도
        val fixedBackOff = FixedBackOff(1000L, 3)

        // Dead Letter Topic으로 실패한 메시지 전송
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            TopicPartition(KafkaTopics.DEAD_LETTER_TOPIC, record.partition())
        }

        // 에러 핸들러 생성 및 설정
        val errorHandler = DefaultErrorHandler(recoverer, fixedBackOff)

        // 특정 예외는 재시도하지 않도록 설정
        errorHandler.addNotRetryableExceptions(
            IllegalArgumentException::class.java,
            IllegalStateException::class.java
        )

        return errorHandler
    }

}
