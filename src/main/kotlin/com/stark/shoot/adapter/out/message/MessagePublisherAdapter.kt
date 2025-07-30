package com.stark.shoot.adapter.out.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.`in`.rest.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.MessagePublisherPort
import com.stark.shoot.application.port.out.message.MessageStatusNotificationPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageDomainService
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.event.MentionEvent
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.domain.event.MessageSentEvent
import com.stark.shoot.application.port.out.user.UserQueryPort
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
    private val messageDomainService: MessageDomainService,
    private val eventPublisher: EventPublisher,
    private val userQueryPort: UserQueryPort
) : MessagePublisherPort, MessageStatusNotificationPort {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val KAFKA_CHAT_MESSAGES_TOPIC = "chat-messages"
    }

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
                // 내부 suspend 함수를 활용하여 발행 로직 재사용
                val event = messageDomainService.createMessageEvent(domainMessage)
                publishMessageInternal(request, event, tempId)

                // 도메인 이벤트 발행 (트랜잭션 컨텍스트에서 처리됨)
                publishDomainEvents(domainMessage)
            } catch (throwable: Throwable) {
                handlePublishError(request, tempId, throwable)
            }
        }
    }

    /**
     * 메시지 처리 중 발생한 오류를 공통으로 처리합니다.
     */
    override fun handleProcessingError(request: ChatMessageRequest, throwable: Throwable) {
        // 공통 오류 처리 로직 재사용
        handlePublishError(request, request.tempId.orEmpty(), throwable)
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
        MessageStatusResponse(
            tempId = tempId,
            status = status.name,
            persistedId = null,
            errorMessage = errorMessage,
            createdAt = Instant.now().toString()
        ).let { statusUpdate ->
            webSocketMessageBroker.sendMessage("/topic/message/status/$roomId", statusUpdate)
        }
    }

    /**
     * 메시지 처리 중 발생한 오류를 알립니다.
     */
    override fun notifyMessageError(roomId: Long, throwable: Throwable) {
        ErrorResponse(
            status = 500,
            message = throwable.message ?: "메시지 처리 중 오류가 발생했습니다",
            timestamp = System.currentTimeMillis()
        ).let { errorResponse ->
            webSocketMessageBroker.sendMessage("/topic/errors/$roomId", errorResponse)
        }
    }


    /**
     * 메시지 발행을 위한 내부 공통 함수
     * Redis와 Kafka에 메시지를 발행하고 상태를 업데이트합니다.
     */
    private suspend fun publishMessageInternal(
        request: ChatMessageRequest,
        event: MessageEvent,
        tempId: String
    ) {
        // Redis 발행
        publishToRedis(request)

        // Kafka 발행
        publishToKafka(
            topic = KAFKA_CHAT_MESSAGES_TOPIC,
            key = request.roomId.toString(),
            event = event
        )

        // 상태 업데이트
        notifyMessageStatus(request.roomId, tempId, MessageStatus.SENT_TO_KAFKA)
    }

    private fun handlePublishError(message: ChatMessageRequest, tempId: String, throwable: Throwable) {
        // 로그 기록
        logger.error(throwable) { "메시지 처리 실패: roomId=${message.roomId}, content=${message.content.text}" }

        // 오류 알림
        notifyMessageError(message.roomId, throwable)

        // 상태 업데이트 (tempId가 있는 경우만)
        tempId.takeIf { it.isNotEmpty() }?.let {
            notifyMessageStatus(message.roomId, it, MessageStatus.FAILED, throwable.message)
        }
    }

    /**
     * Redis에 메시지를 직접 발행합니다.
     *
     * @param message 발행할 메시지 요청
     */
    private suspend fun publishToRedis(message: ChatMessageRequest) {
        val streamKey = "stream:chat:room:${message.roomId}"
        try {
            val messageJson = objectMapper.writeValueAsString(message)
            val record = StreamRecords.newRecord()
                .ofMap(mapOf("message" to messageJson))
                .withStreamKey(streamKey)

            redisTemplate.opsForStream<String, String>().add(record)
                .also { messageId -> logger.debug { "Redis Stream에 메시지 발행 완료: $streamKey, id: $messageId" } }
        } catch (e: Exception) {
            logger.error(e) { "Redis Stream 발행 실패: $streamKey, ${e.message}" }
            throw e
        }
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
            kafkaTemplate.send(topic, key, event).await()
                .also { result ->
                    logger.debug { "Kafka 메시지 발행 완료: topic=$topic, key=$key, offset=${result.recordMetadata.offset()}" }
                }
        } catch (ex: Exception) {
            logger.error(ex) { "Kafka 메시지 발행 실패: topic=$topic, key=$key, ${ex.message}" }
            throw KafkaPublishException("Kafka 메시지 발행 실패", ex)
        }
    }

    /**
     * 도메인 이벤트를 발행합니다.
     * MessageSentEvent와 필요시 MentionEvent를 발행합니다.
     */
    private fun publishDomainEvents(message: ChatMessage) {
        try {
            // 1. MessageSentEvent 발행
            val messageSentEvent = MessageSentEvent.create(message)
            eventPublisher.publish(messageSentEvent)

            // 2. 멘션이 포함된 경우 MentionEvent 발행
            if (message.mentions.isNotEmpty()) {
                publishMentionEvent(message)
            }

            logger.debug { "Domain events published for message ${message.id?.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish domain events for message ${message.id?.value}" }
        }
    }

    /**
     * 멘션 이벤트를 발행합니다.
     */
    private fun publishMentionEvent(message: ChatMessage) {
        // 자신을 멘션한 경우는 제외
        val mentionedUsers = message.mentions.filter { it != message.senderId }.toSet()
        if (mentionedUsers.isEmpty()) {
            return
        }

        // 발신자 정보 조회 (실패 시 기본 값 사용)
        val senderName = userQueryPort
            .findUserById(message.senderId)
            ?.nickname
            ?.value
            ?: "User_${message.senderId.value}"

        val mentionEvent = MentionEvent(
            roomId = message.roomId,
            messageId = message.id ?: return,
            senderId = message.senderId,
            senderName = senderName,
            mentionedUserIds = mentionedUsers,
            messageContent = message.content.text
        )

        eventPublisher.publish(mentionEvent)
        logger.debug { "MentionEvent published for ${mentionedUsers.size} users" }
    }

}
