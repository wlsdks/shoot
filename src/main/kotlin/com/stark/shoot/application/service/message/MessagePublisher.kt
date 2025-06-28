package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.kafka.KafkaMessagePublishPort
import com.stark.shoot.application.port.out.message.PublishMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageDomainService
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import com.stark.shoot.infrastructure.exception.web.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

/**
 * 메시지를 Redis와 Kafka에 발행하고 상태 알림을 담당하는 서비스
 */
@UseCase
class MessagePublisher(
    private val publishMessagePort: PublishMessagePort,
    private val kafkaMessagePublishPort: KafkaMessagePublishPort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val applicationCoroutineScope: ApplicationCoroutineScope,
    private val messageDomainService: MessageDomainService
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 발행합니다.
     *
     * @param request  메시지 요청 DTO
     * @param domainMessage 도메인 메시지
     */
    fun publish(request: ChatMessageRequest, domainMessage: ChatMessage) {
        applicationCoroutineScope.launch {
            val tempId = domainMessage.metadata.tempId ?: ""
            try {
                // Redis 발행
                publishMessagePort.publish(request)

                // Kafka 발행
                val event = messageDomainService.createMessageEvent(domainMessage)
                kafkaMessagePublishPort.publishChatEventSuspend(
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
    fun handleProcessingError(request: ChatMessageRequest, throwable: Throwable) {
        handlePublishError(request, request.tempId ?: "", throwable)
    }

    private fun handlePublishError(message: ChatMessageRequest, tempId: String, throwable: Throwable) {
        logMessageError(message, throwable)
        notifyMessageError(message.roomId, throwable)
        if (tempId.isNotEmpty()) {
            notifyMessageStatus(message.roomId, tempId, MessageStatus.FAILED, throwable.message)
        }
    }

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

    private fun logMessageError(message: ChatMessageRequest, throwable: Throwable) {
        logger.error(throwable) { "메시지 처리 실패: roomId=${'$'}{message.roomId}, content=${'$'}{message.content.text}" }
    }

    private fun notifyMessageError(roomId: Long, throwable: Throwable) {
        val errorResponse = ErrorResponse(
            status = 500,
            message = throwable.message ?: "메시지 처리 중 오류가 발생했습니다",
            timestamp = System.currentTimeMillis()
        )
        webSocketMessageBroker.sendMessage("/topic/errors/$roomId", errorResponse)
    }

    private fun determineKafkaTopic(): String = "chat-messages"
}

