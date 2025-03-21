package com.stark.shoot.adapter.`in`.web.socket.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import com.stark.shoot.infrastructure.exception.web.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.time.Instant
import java.util.*

@Controller
class MessageStompHandler(
    private val sendMessageUseCase: SendMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val extractUrlPort: ExtractUrlPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val applicationCoroutineScope: ApplicationCoroutineScope
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 클라이언트로부터 메시지를 수신하여 처리합니다.
     * 1. 메시지에 임시 ID와 "sending" 상태 추가
     * 2. Redis를 통해 메시지 즉시 브로드캐스트 (실시간성)
     * 3. Kafka를 통해 메시지 영속화 (안정성)
     * 4. 메시지 상태 업데이트를 클라이언트에 전송
     */
    @Operation(
        summary = "클라이언트로부터 메시지를 수신하여 Redis, Kafka로 전달하여 처리합니다.",
        description = """
            - 웹소켓으로 메시지를 받으면 Redis(Pub/Sub)로 실시간 전송을 하고 Kafka로 mongoDB에 저장합니다.
              - 1. 메시지에 임시 ID와 "sending" 상태 추가 (임시 상태를 웹소켓으로 보내서 프론트에서 상태 제어: 전송중, 실패 등)
              - 2. Redis를 통해 메시지 즉시 브로드캐스트    (일단 실시간으로 웹소켓으로 상대방에게 메시지 전송)
              - 3. Kafka를 통해 메시지 영속화            (메시지 저장을 보장하기 위해서 분리)
              - 4. 메시지 상태 업데이트를 클라이언트에 전송   (최종 저장되면 SAVED 상태 전송)
        """
    )
    @MessageMapping("/chat")
    fun handleChatMessage(message: ChatMessageRequest) {
        try {
            // 1. 메시지에 임시 ID 추가
            val tempId = UUID.randomUUID().toString()
            message.apply {
                this.tempId = tempId
                this.status = MessageStatus.SENDING.name
                this.metadata["tempId"] = tempId
            }

            // 2. URL 미리보기 처리 (캐시 확인만)
            if (message.content.type == "TEXT") {
                val urls = extractUrlPort.extractUrls(message.content.text)
                if (urls.isNotEmpty()) {
                    val url = urls.first()
                    val cachedPreview = cacheUrlPreviewPort.getCachedUrlPreview(url)

                    // 캐시된 미리보기가 있으면 메시지에 추가
                    if (cachedPreview != null) {
                        message.metadata["urlPreview"] = objectMapper.writeValueAsString(cachedPreview)
                    } else {
                        // 캐시 미스인 경우 처리 필요 표시
                        message.metadata["needsUrlPreview"] = "true"
                        message.metadata["previewUrl"] = url
                    }
                }
            }

            // 3. Redis와 Kafka 발행 - 코루틴으로 변경
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
     * Redis를 통해 메시지를 실시간으로 발행합니다.
     *
     * @param message 메시지
     */
    private suspend fun publishToRedisSuspend(
        message: ChatMessageRequest
    ) {
        withContext(Dispatchers.IO) {
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
    }

    /**
     * Kafka를 통해 메시지를 발행하고 상태를 업데이트합니다.
     *
     * @param message 메시지
     */
    private suspend fun sendToKafkaSuspend(
        message: ChatMessageRequest
    ) = withContext(Dispatchers.IO) {
        val tempId = message.tempId ?: ""

        try {
            // 코루틴 컨텍스트 내에서 다른 suspend 함수 호출
            val result = sendMessageUseCase.handleMessageSuspend(message)

            // 상태 업데이트 (메시징 작업도 I/O 작업)
            val statusUpdate = MessageStatusResponse(
                tempId = tempId,
                status = MessageStatus.SENT_TO_KAFKA.name, // Kafka로 전송됨 (아직 DB에 저장되지 않음)
                persistedId = null,
                errorMessage = null,
                createdAt = Instant.now().toString()
            )
            messagingTemplate.convertAndSend("/topic/message/status/${message.roomId}", statusUpdate)
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
        messagingTemplate.convertAndSend("/topic/errors/${message.roomId}", errorResponse)

        // 2. 메시지 상태 업데이트 전송
        val statusUpdate = MessageStatusResponse(
            tempId = tempId,
            status = MessageStatus.FAILED.name,
            persistedId = null,
            errorMessage = throwable.message,
            createdAt = Instant.now().toString()
        )
        messagingTemplate.convertAndSend("/topic/message/status/${message.roomId}", statusUpdate)
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
        messagingTemplate.convertAndSend("/topic/errors/${message.roomId}", errorResponse)
    }
}