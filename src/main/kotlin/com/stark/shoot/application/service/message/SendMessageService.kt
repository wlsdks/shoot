package com.stark.shoot.application.service.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.application.port.out.kafka.KafkaMessagePublishPort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import com.stark.shoot.infrastructure.exception.web.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Instant
import java.util.*

@UseCase
class SendMessageService(
    private val extractUrlPort: ExtractUrlPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val kafkaMessagePublishPort: KafkaMessagePublishPort,
    private val objectMapper: ObjectMapper,
    private val redisTemplate: StringRedisTemplate,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val applicationCoroutineScope: ApplicationCoroutineScope,
    private val messageDomainService: com.stark.shoot.domain.service.message.MessageDomainService
) : SendMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 전송합니다.
     * 1. 도메인 객체 생성 및 비즈니스 로직 처리
     * 2. 메시지 발행 (Redis, Kafka)
     *
     * @param messageRequest 메시지 요청 DTO
     */
    override fun sendMessage(messageRequest: ChatMessageRequest) {
        try {
            // 1. 도메인 객체 생성 및 비즈니스 로직 처리
            val domainMessage = createAndProcessDomainMessage(messageRequest)

            // 2. 메시지 발행 (Redis, Kafka)
            publishMessage(messageRequest, domainMessage)
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 중 예외 발생: ${e.message}" }
            sendErrorResponse(e, messageRequest)
        }
    }

    /**
     * 도메인 메시지 객체를 생성하고 비즈니스 로직을 처리합니다.
     *
     * @param messageRequest 메시지 요청 DTO
     * @return 처리된 도메인 메시지 객체
     */
    private fun createAndProcessDomainMessage(
        messageRequest: ChatMessageRequest
    ): ChatMessage {
        // 도메인 서비스를 사용하여 메시지 생성 및 처리
        val messageWithPreview = messageDomainService.createAndProcessMessage(
            messageRequest = messageRequest,
            extractUrls = { text -> extractUrlPort.extractUrls(text) },
            getCachedPreview = { url -> cacheUrlPreviewPort.getCachedUrlPreview(url) }
        )

        // 요청 객체에 도메인 처리 결과 반영
        messageDomainService.updateRequestFromDomain(messageRequest, messageWithPreview)

        return messageWithPreview
    }

    /**
     * 메시지를 발행합니다 (Redis, Kafka).
     *
     * @param messageRequest 메시지 요청 DTO
     * @param domainMessage 도메인 메시지 객체
     */
    private fun publishMessage(
        messageRequest: ChatMessageRequest,
        domainMessage: ChatMessage
    ) {
        applicationCoroutineScope.launch {
            try {
                // 1. Redis 발행
                publishToRedisSuspend(messageRequest)

                // 2. Kafka 발행과 상태 업데이트
                sendToKafkaSuspend(messageRequest)
            } catch (throwable: Throwable) {
                handleMessageError(messageRequest, domainMessage.metadata.tempId ?: "", throwable)
            }
        }
    }

    // 도메인 서비스로 이동됨

    /**
     * Redis를 통해 메시지를 실시간으로 발행합니다.
     * 이 메서드는 인프라스트럭처 계층과의 통신을 담당합니다.
     *
     * @param message 메시지 요청 DTO
     */
    private suspend fun publishToRedisSuspend(
        message: ChatMessageRequest
    ) {
        val streamKey = generateStreamKey(message.roomId)
        try {
            val messageId = publishToRedisStream(streamKey, message)
            logger.debug { "Redis Stream에 메시지 발행: $streamKey, id: $messageId, tempId: ${message.tempId}" }
        } catch (e: Exception) {
            logger.error(e) { "Redis 발행 실패: ${e.message}" }
            throw e
        }
    }

    /**
     * Redis 스트림 키를 생성합니다.
     *
     * @param roomId 채팅방 ID
     * @return Redis 스트림 키
     */
    private fun generateStreamKey(roomId: Long): String {
        return "stream:chat:room:$roomId"
    }

    /**
     * Redis 스트림에 메시지를 발행합니다.
     *
     * @param streamKey Redis 스트림 키
     * @param message 메시지 요청 DTO
     * @return 발행된 메시지 ID
     */
    private suspend fun publishToRedisStream(streamKey: String, message: ChatMessageRequest): Any {
        val messageJson = objectMapper.writeValueAsString(message)
        val map = mapOf("message" to messageJson)

        // StreamRecords 사용
        val record = StreamRecords.newRecord()
            .ofMap(map)
            .withStreamKey(streamKey)

        // Stream 추가
        return redisTemplate.opsForStream<String, String>()
            .add(record) ?: "unknown-id"
    }

    /**
     * Kafka를 통해 메시지를 발행하고 상태를 업데이트합니다.
     * 이 메서드는 인프라스트럭처 계층과의 통신을 담당합니다.
     *
     * @param message 메시지 요청 DTO
     */
    private suspend fun sendToKafkaSuspend(
        message: ChatMessageRequest
    ) {
        val tempId = message.tempId ?: ""

        try {
            // 1. 메시지 이벤트 발행
            publishMessageToKafka(message)

            // 2. 상태 업데이트 전송
            notifyMessageStatus(message.roomId, tempId, MessageStatus.SENT_TO_KAFKA)

            logger.debug { "메시지 Kafka 발행 성공, 상태 업데이트: tempId=$tempId" }
        } catch (e: Exception) {
            logger.error(e) { "Kafka 발행 실패: ${e.message}" }
            throw e
        }
    }

    /**
     * 메시지를 Kafka에 발행합니다.
     *
     * @param message 메시지 요청 DTO
     * @return 발행 결과
     */
    private suspend fun publishMessageToKafka(
        message: ChatMessageRequest
    ): String? {
        // 도메인 객체 생성 및 이벤트 발행
        return handleMessageSuspend(message)
    }

    /**
     * 메시지 상태를 클라이언트에 알립니다.
     *
     * @param roomId 채팅방 ID
     * @param tempId 임시 메시지 ID
     * @param status 메시지 상태
     */
    private fun notifyMessageStatus(
        roomId: Long,
        tempId: String,
        status: MessageStatus
    ) {
        val statusUpdate = MessageStatusResponse(
            tempId = tempId,
            status = status.name,
            persistedId = null,
            errorMessage = null,
            createdAt = Instant.now().toString()
        )

        webSocketMessageBroker.sendMessage("/topic/message/status/$roomId", statusUpdate)
    }

    /**
     * 메시지 처리 중 오류가 발생했을 때 처리합니다.
     * 오류 로깅 및 클라이언트 알림을 담당합니다.
     *
     * @param message 메시지 요청 DTO
     * @param tempId 임시 ID
     * @param throwable 예외
     */
    private fun handleMessageError(
        message: ChatMessageRequest,
        tempId: String,
        throwable: Throwable
    ) {
        // 공통 오류 처리 메서드 호출
        handleMessageProcessingError(message, tempId, throwable)
    }

    /**
     * 메시지 처리 오류를 로깅합니다.
     *
     * @param message 메시지 요청 DTO
     * @param throwable 예외
     */
    private fun logMessageError(
        message: ChatMessageRequest,
        throwable: Throwable
    ) {
        logger.error(throwable) { "메시지 처리 실패: roomId=${message.roomId}, content=${message.content.text}" }
    }

    /**
     * 오류를 클라이언트에 알립니다.
     *
     * @param roomId 채팅방 ID
     * @param throwable 예외
     */
    private fun notifyMessageError(
        roomId: Long,
        throwable: Throwable
    ) {
        val errorResponse = ErrorResponse(
            status = 500,
            message = throwable.message ?: "메시지 처리 중 오류가 발생했습니다",
            timestamp = System.currentTimeMillis()
        )
        webSocketMessageBroker.sendMessage("/topic/errors/$roomId", errorResponse)
    }

    /**
     * 메시지 상태를 클라이언트에 알립니다.
     *
     * @param roomId 채팅방 ID
     * @param tempId 임시 메시지 ID
     * @param status 메시지 상태
     * @param errorMessage 오류 메시지 (선택적)
     */
    private fun notifyMessageStatus(
        roomId: Long,
        tempId: String,
        status: MessageStatus,
        errorMessage: String? = null
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
     * 메시지 전송 처리 (Kafka로 이벤트 발행)
     * 도메인 객체 생성 및 이벤트 발행을 담당합니다.
     *
     * @param requestMessage 메시지 요청 DTO
     * @return 임시 메시지 ID
     */
    private suspend fun handleMessageSuspend(
        requestMessage: ChatMessageRequest
    ): String? {
        // 1. 도메인 객체 생성
        val chatMessage = createDomainMessage(requestMessage)

        // 2. 도메인 이벤트 생성
        val chatEvent = createDomainEvent(chatMessage)

        // 3. Kafka로 이벤트 발행
        return publishKafkaMessageSuspend(requestMessage.roomId, chatEvent, requestMessage.tempId)
    }

    /**
     * 요청 DTO로부터 도메인 메시지 객체를 생성합니다.
     *
     * @param requestMessage 메시지 요청 DTO
     * @return 도메인 메시지 객체
     */
    private fun createDomainMessage(
        requestMessage: ChatMessageRequest
    ): ChatMessage {
        return ChatMessage.fromRequest(requestMessage)
    }

    /**
     * 도메인 메시지로부터 도메인 이벤트를 생성합니다.
     *
     * @param chatMessage 도메인 메시지 객체
     * @return 도메인 이벤트
     */
    private fun createDomainEvent(
        chatMessage: ChatMessage
    ): ChatEvent {
        return messageDomainService.createMessageEvent(chatMessage)
    }

    /**
     * Kafka에 도메인 이벤트를 발행합니다.
     *
     * @param roomId 채팅방 ID
     * @param chatEvent 도메인 이벤트
     * @param tempId 임시 메시지 ID
     * @return 임시 메시지 ID
     */
    private suspend fun publishKafkaMessageSuspend(
        roomId: Long,
        chatEvent: ChatEvent,
        tempId: String?
    ): String? {
        try {
            // Kafka 토픽 및 키 생성
            val topic = determineKafkaTopic()
            val key = roomId.toString()

            // 이벤트 발행
            kafkaMessagePublishPort.publishChatEventSuspend(
                topic = topic,
                key = key,
                event = chatEvent
            )
            return tempId
        } catch (e: Exception) {
            // 에러 로깅
            logger.error(e) { "Kafka 발행 실패: roomId=$roomId" }
            // 예외 전파
            throw e
        }
    }

    /**
     * Kafka 토픽을 결정합니다.
     *
     * @return Kafka 토픽 이름
     */
    private fun determineKafkaTopic(): String {
        return "chat-messages"
    }

    /**
     * 에러 응답을 클라이언트에 전송합니다.
     *
     * @param e 예외
     * @param message 요청 메시지
     */
    private fun sendErrorResponse(
        e: Exception,
        message: ChatMessageRequest
    ) {
        // 메시지 처리 오류 처리 공통 메서드 호출
        handleMessageProcessingError(message, message.tempId ?: "", e)
    }

    /**
     * 메시지 처리 중 발생한 오류를 공통으로 처리합니다.
     * 
     * @param message 메시지 요청 DTO
     * @param tempId 임시 메시지 ID
     * @param throwable 발생한 예외
     */
    private fun handleMessageProcessingError(
        message: ChatMessageRequest,
        tempId: String,
        throwable: Throwable
    ) {
        // 1. 오류 로깅
        logMessageError(message, throwable)

        // 2. 오류 알림
        notifyMessageError(message.roomId, throwable)

        // 3. 메시지 상태 업데이트
        if (tempId.isNotEmpty()) {
            notifyMessageStatus(
                roomId = message.roomId,
                tempId = tempId,
                status = MessageStatus.FAILED,
                errorMessage = throwable.message
            )
        }
    }

}
