package com.stark.shoot.adapter.out.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.message.MessagePublisherPort
import com.stark.shoot.application.port.out.message.MessageStatusNotificationPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageDomainService
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import com.stark.shoot.infrastructure.exception.web.ErrorResponse
import com.stark.shoot.infrastructure.exception.web.KafkaPublishException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.kafka.core.KafkaTemplate
import java.time.Instant

/**
 * 메시지를 Redis와 Kafka에 발행하고 상태 알림을 담당하는 어댑터
 */
@Adapter
class MessagePublisherAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val kafkaTemplate: KafkaTemplate<String, MessageEvent>,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val applicationCoroutineScope: ApplicationCoroutineScope,
    private val messageDomainService: MessageDomainService
) : MessagePublisherPort, MessageStatusNotificationPort {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 발행합니다.
     *
     * @param request 메시지 요청 DTO
     * @param domainMessage 도메인 메시지
     */
    override fun publish(request: ChatMessageRequest, domainMessage: ChatMessage) {
        applicationCoroutineScope.launch {
            val tempId = domainMessage.metadata.tempId ?: ""
            try {
                // Redis 발행 - 직접 Redis 인프라 사용
                publishToRedis(request)

                // Kafka 발행 - 직접 Kafka 인프라 사용
                val event = messageDomainService.createMessageEvent(domainMessage)
                publishToKafka(
                    topic = determineKafkaTopic(),
                    key = request.roomId.toString(),
                    event = event
                )

                // 상태 업데이트
                notifyMessageStatus(request.roomId, tempId, MessageStatus.SENT_TO_KAFKA)
            } catch (throwable: Throwable) {
                handlePublishError(request, tempId, throwable)
            }
        }
    }

    /**
     * 메시지 처리 중 발생한 오류를 공통으로 처리합니다.
     */
    override fun handleProcessingError(request: ChatMessageRequest, throwable: Throwable) {
        handlePublishError(request, request.tempId ?: "", throwable)
    }

    /**
     * 메시지 상태 변경을 알립니다.
     */
    override fun notifyMessageStatus(
        roomId: Long,
        tempId: String,
        status: MessageStatus,
        errorMessage: String?
    ) {
        val statusUpdate = MessageStatusResponse(
            tempId = tempId,
            status = status.name,
            persistedId = null,
            errorMessage = errorMessage,
            createdAt = Instant.now().toString()
        )
        webSocketMessageBroker.sendMessage("/topic/message/status/$roomId", statusUpdate)
    }

    /**
     * 메시지 처리 중 발생한 오류를 알립니다.
     */
    override fun notifyMessageError(roomId: Long, throwable: Throwable) {
        val errorResponse = ErrorResponse(
            status = 500,
            message = throwable.message ?: "메시지 처리 중 오류가 발생했습니다",
            timestamp = System.currentTimeMillis()
        )
        webSocketMessageBroker.sendMessage("/topic/errors/$roomId", errorResponse)
    }

    private fun handlePublishError(message: ChatMessageRequest, tempId: String, throwable: Throwable) {
        logMessageError(message, throwable)
        notifyMessageError(message.roomId, throwable)
        if (tempId.isNotEmpty()) {
            notifyMessageStatus(message.roomId, tempId, MessageStatus.FAILED, throwable.message)
        }
    }

    private fun logMessageError(message: ChatMessageRequest, throwable: Throwable) {
        logger.error(throwable) { "메시지 처리 실패: roomId=${message.roomId}, content=${message.content.text}" }
    }

    private fun determineKafkaTopic(): String = "chat-messages"

    /**
     * Redis에 메시지를 직접 발행합니다.
     *
     * @param message 발행할 메시지 요청
     */
    private suspend fun publishToRedis(message: ChatMessageRequest) {
        val streamKey = "stream:chat:room:${message.roomId}"
        val messageJson = objectMapper.writeValueAsString(message)
        val map = mapOf("message" to messageJson)
        val record = StreamRecords.newRecord().ofMap(map).withStreamKey(streamKey)
        redisTemplate.opsForStream<String, String>().add(record)
    }

    /**
     * Kafka에 메시지를 직접 발행합니다.
     *
     * @param topic 발행할 토픽
     * @param key 메시지 키
     * @param event 발행할 이벤트
     */
    private suspend fun publishToKafka(topic: String, key: String, event: MessageEvent) {
        try {
            val result = kafkaTemplate.send(topic, key, event).await()
            // logger.info { "Message sent to topic: $topic, key: $key, offset: ${result.recordMetadata.offset()}" }
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to send message to topic: $topic, key: $key, event: $event" }
            throw KafkaPublishException("Failed to publish message to Kafka", ex)
        }
    }
}
