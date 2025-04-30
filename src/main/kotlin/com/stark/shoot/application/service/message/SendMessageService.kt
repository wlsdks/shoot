package com.stark.shoot.application.service.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.application.port.out.kafka.KafkaMessagePublishPort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import com.stark.shoot.infrastructure.exception.web.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val applicationCoroutineScope: ApplicationCoroutineScope
) : SendMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 전송합니다.
     * 1. 메시지에 임시 ID와 상태 추가
     * 2. URL 미리보기 처리 (캐시 확인)
     * 3. Redis와 Kafka 발행
     *
     * @param message 메시지
     */
    override fun sendMessage(message: ChatMessageRequest) {
        try {
            // 1. 메시지 전송 준비 (임시 ID와 상태 설정)
            prepareForSending(message)

            // 임시 ID 변수 (에러 처리용)
            val tempId = message.tempId ?: UUID.randomUUID().toString()

            // 3. URL 미리보기 처리 (캐시 확인만)
            getCachedUrlPreview(message)

            // 4. Redis와 Kafka 발행 - 코루틴으로 변경
            applicationCoroutineScope.launch {
                try {
                    // Redis 발행
                    publishToRedisSuspend(message)

                    // Kafka 발행과 상태 업데이트
                    sendToKafkaSuspend(message)
                } catch (throwable: Throwable) {
                    handleMessageError(message, tempId, throwable)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 중 예외 발생: ${e.message}" }
            sendErrorResponse(e, message)
        }
    }

    /**
     * 메시지 전송을 위해 준비합니다.
     * 임시 ID와 상태를 설정합니다.
     *
     * @param request 메시지 요청
     * @return 업데이트된 메시지 요청
     */
    private fun prepareForSending(request: ChatMessageRequest): ChatMessageRequest {
        // 임시 ID 생성
        val tempId = java.util.UUID.randomUUID().toString()

        // 메시지에 임시 ID와 상태 추가
        return request.apply {
            this.tempId = tempId
            this.status = MessageStatus.SENDING
        }
    }

    /**
     * ChatMessageRequest로부터 ChatMessage 객체를 생성합니다.
     *
     * @param request ChatMessageRequest
     * @return ChatMessage
     */
    fun fromRequest(request: ChatMessageRequest): ChatMessage {
        val chatMessage = ChatMessage(
            roomId = request.roomId,
            senderId = request.senderId,
            content = MessageContent(
                text = request.content.text,
                type = MessageType.TEXT
            ),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )

        // 메타데이터 복사 (tempId와 status 포함)
        if (request.metadata != null) {
            chatMessage.metadata = chatMessage.metadata.requestToDomain(request.metadata)
        }

        return chatMessage
    }

    /**
     * URL 미리보기를 처리합니다.
     * URL 추출 및 미리보기 처리를 수행합니다.
     *
     * @param message 메시지
     */
    private fun getCachedUrlPreview(message: ChatMessageRequest) {
        if (message.content.type == MessageType.TEXT) {
            val urls = extractUrlPort.extractUrls(message.content.text)
            if (urls.isNotEmpty()) {
                val url = urls.first()
                val cachedPreview = cacheUrlPreviewPort.getCachedUrlPreview(url)

                // 캐시된 미리보기가 있으면 메시지에 추가
                if (cachedPreview != null) {
                    message.metadata.urlPreview = cachedPreview
                } else {
                    // 캐시 미스인 경우 처리 필요 표시
                    message.metadata.needsUrlPreview = true
                    message.metadata.previewUrl = url
                }
            }
        }
    }

    /**
     * Redis를 통해 메시지를 실시간으로 발행합니다.
     *
     * @param message 메시지
     */
    private suspend fun publishToRedisSuspend(
        message: ChatMessageRequest
    ) {
        val streamKey = "stream:chat:room:${message.roomId}"
        try {
            val messageJson = objectMapper.writeValueAsString(message)
            val map = mapOf("message" to messageJson)

            // StreamRecords 사용
            val record = StreamRecords.newRecord()
                .ofMap(map)
                .withStreamKey(streamKey)

            // Stream 추가
            val messageId = redisTemplate.opsForStream<String, String>()
                .add(record)

            logger.debug { "Redis Stream에 메시지 발행: $streamKey, id: $messageId, tempId: ${message.tempId}" }
        } catch (e: Exception) {
            logger.error(e) { "Redis 발행 실패: ${e.message}" }
            throw e
        }
    }

    /**
     * Kafka를 통해 메시지를 발행하고 상태를 업데이트합니다.
     *
     * @param message 메시지
     */
    private suspend fun sendToKafkaSuspend(
        message: ChatMessageRequest
    ) {
        val tempId = message.tempId ?: ""

        try {
            // 코루틴 컨텍스트 내에서 다른 suspend 함수 호출
            val result = handleMessageSuspend(message)

            // 상태 업데이트 (메시징 작업도 I/O 작업)
            val statusUpdate = MessageStatusResponse(
                tempId = tempId,
                status = MessageStatus.SENT_TO_KAFKA.name, // Kafka로 전송됨 (아직 DB에 저장되지 않음)
                persistedId = null,
                errorMessage = null,
                createdAt = Instant.now().toString()
            )

            webSocketMessageBroker.sendMessage("/topic/message/status/${message.roomId}", statusUpdate)
            logger.debug { "메시지 Kafka 발행 성공, 상태 업데이트: tempId=$tempId" }
        } catch (e: Exception) {
            logger.error(e) { "Kafka 발행 실패: ${e.message}" }
            throw e
        }
    }

    /**
     * 메시지 처리 중 오류가 발생했을 때 처리합니다.
     *
     * @param message 메시지
     * @param tempId 임시 ID
     * @param throwable 예외
     */
    private fun handleMessageError(
        message: ChatMessageRequest,
        tempId: String,
        throwable: Throwable
    ) {
        logger.error(throwable) { "Kafka 발행 실패: ${message.content.text}" }

        // 1. 일반 오류 채널로 전송
        val errorResponse = ErrorResponse(
            status = 500,
            message = throwable.message ?: "메시지 처리 중 오류가 발생했습니다",
            timestamp = System.currentTimeMillis()
        )
        webSocketMessageBroker.sendMessage("/topic/errors/${message.roomId}", errorResponse)

        // 2. 메시지 상태 업데이트 전송
        val statusUpdate = MessageStatusResponse(
            tempId = tempId,
            status = MessageStatus.FAILED.name,
            persistedId = null,
            errorMessage = throwable.message,
            createdAt = Instant.now().toString()
        )
        webSocketMessageBroker.sendMessage("/topic/message/status/${message.roomId}", statusUpdate)
    }


    /**
     * 메시지 전송 처리 (Kafka로 이벤트 발행)
     *
     * @param requestMessage ChatMessageRequest
     * @return CompletableFuture<String?>
     */
    private suspend fun handleMessageSuspend(
        requestMessage: ChatMessageRequest
    ): String? {
        // ChatMessageRequest로부터 ChatMessage 생성 후 ChatEvent 생성
        val chatMessage = fromRequest(requestMessage)
        val chatEvent = ChatEvent.fromMessage(chatMessage, EventType.MESSAGE_CREATED)

        // Kafka로 이벤트 발행 (코루틴 방식)
        return publishKafkaMessageSuspend(requestMessage, chatEvent, requestMessage.tempId)
    }


    // createChatMessage와 createChatEvent 메서드는 이제 이 서비스에 구현되어 있습니다.
    // fromRequest 메서드와 ChatEvent.fromMessage 메서드를 사용하세요.

    private suspend fun publishKafkaMessageSuspend(
        requestMessage: ChatMessageRequest,
        chatEvent: ChatEvent,
        tempId: String?
    ): String? {
        try {
            kafkaMessagePublishPort.publishChatEventSuspend(
                topic = "chat-messages",
                key = requestMessage.roomId.toString(),
                event = chatEvent
            )
            return tempId
        } catch (e: Exception) {
            // 에러 로깅
            logger.error(e) { "Kafka 발행 실패: ${requestMessage.roomId}" }
            // 예외 전파
            throw e
        }
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
        val errorResponse = ErrorResponse(
            status = 500,
            message = e.message ?: "메시지 처리 중 오류가 발생했습니다",
            timestamp = System.currentTimeMillis()
        )

        webSocketMessageBroker.sendMessage(
            "/topic/errors/${message.roomId}",
            errorResponse
        )
    }

}
