package com.stark.shoot.adapter.`in`.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageMetadata
import com.stark.shoot.domain.chat.message.UrlPreview
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MessageKafkaConsumer(
    private val processMessageUseCase: ProcessMessageUseCase,
    private val loadUrlContentPort: LoadUrlContentPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val messagingTemplate: SimpMessagingTemplate,
    private val chatMessageMapper: ChatMessageMapper,
    private val objectMapper: ObjectMapper,
    private val appCoroutineScope: ApplicationCoroutineScope
) {
    private val logger = KotlinLogging.logger {}

    @KafkaListener(topics = ["chat-messages"], groupId = "shoot")
    fun consumeMessage(@Payload event: ChatEvent) {
        if (event.type == EventType.MESSAGE_CREATED) {
            // 코루틴 내부에서 비동기 처리
            appCoroutineScope.launch {
                try {
                    // 메시지 내부의 임시 ID, 채팅방 ID 추출
                    val tempId = event.data.metadata["tempId"] as? String ?: return@launch
                    val roomId = event.data.roomId

                    // MongoDB 저장 전 처리 중 상태 업데이트
                    sendStatusUpdate(roomId, tempId, MessageStatus.PROCESSING.name, null)

                    // 메시지 저장
                    val savedMessage = processMessageUseCase.processMessageCreate(event.data)

                    // 저장 성공 상태 업데이트
                    sendStatusUpdate(roomId, tempId, MessageStatus.SAVED.name, savedMessage.id)

                    // URL 미리보기 처리 필요 여부 확인
                    if (savedMessage.metadata.containsKey("needsUrlPreview") &&
                        savedMessage.metadata.containsKey("previewUrl")
                    ) {
                        val previewUrl = savedMessage.metadata["previewUrl"] as String

                        // URL 미리보기 비동기 처리
                        try {
                            val preview = loadUrlContentPort.fetchUrlContent(previewUrl)
                            if (preview != null) {
                                // 캐싱
                                cacheUrlPreviewPort.cacheUrlPreview(previewUrl, preview)

                                // 메시지 업데이트
                                val updatedMessage = updateMessageWithPreview(savedMessage, preview)

                                // 업데이트된 메시지 전송
                                sendMessageUpdate(updatedMessage)
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "URL 미리보기 처리 실패: $previewUrl" }
                        }
                    }
                } catch (e: Exception) {
                    sendErrorResponse(event, e)
                }
            }
        }
    }

    /**
     * 메시지 상태 업데이트를 WebSocket으로 전송합니다.
     *
     * @param roomId 채팅방 ID
     * @param tempId 임시 메시지 ID
     * @param status 상태: "sending", "saved", "failed" 등
     * @param persistedId 영구 저장된 메시지 ID (성공 시)
     * @param errorMessage 오류 메시지 (실패 시)
     */
    private fun sendStatusUpdate(
        roomId: Long,
        tempId: String,
        status: String,
        persistedId: String?,
        errorMessage: String? = null
    ) {
        // 현재 시간을 ISO 형식 문자열로 변환
        val createdAt = Instant.now().toString()

        val statusUpdate = MessageStatusResponse(
            tempId = tempId,
            status = status,
            persistedId = persistedId,
            errorMessage = errorMessage,
            createdAt = createdAt
        )
        messagingTemplate.convertAndSend("/topic/message/status/$roomId", statusUpdate)
    }

    /**
     * 메시지 처리 중 오류 발생 시 클라이언트에 에러 메시지를 전송합니다.
     *
     * @param event ChatEvent 객체
     * @param e Exception 객체
     */
    private fun sendErrorResponse(
        event: ChatEvent,
        e: Exception
    ) {
        val tempId = event.data.metadata["tempId"] as? String

        if (tempId != null) {
            sendStatusUpdate(
                event.data.roomId,
                tempId,
                MessageStatus.FAILED.name,
                null,
                e.message
            )
        }

        logger.error(e) { "메시지 처리 오류: ${e.message}" }
    }

    /**
     * 메시지에 URL 미리보기 정보를 추가합니다.
     *
     * @param message 메시지
     * @param preview URL 미리보기 정보
     * @return 업데이트된 메시지
     */
    private fun updateMessageWithPreview(
        message: ChatMessage,
        preview: UrlPreview
    ): ChatMessage {
        // 메시지 복사 및 미리보기 추가
        val updatedMetadata = message.metadata.toMutableMap().apply {
            remove("needsUrlPreview")
            remove("previewUrl")
            put("urlPreview", objectMapper.writeValueAsString(preview))
        }

        // 메시지 내용에도 미리보기 추가 (MessageContent.metadata.urlPreview)
        val currentContent = message.content
        val currentMetadata = currentContent.metadata ?: MessageMetadata()
        val updatedContentMetadata = currentMetadata.copy(urlPreview = preview)
        val updatedContent = currentContent.copy(metadata = updatedContentMetadata)

        // 업데이트된 메시지
        return message.copy(
            content = updatedContent,
            metadata = updatedMetadata
        )
    }

    /**
     * 수정된 메시지를 WebSocket으로 전송합니다.
     *
     * @param message 수정된 메시지
     */
    private fun sendMessageUpdate(message: ChatMessage) {
        // 수정: ChatMessageMapper를 주입 받아야 함
        val messageDto = chatMessageMapper.toDto(message)

        // WebSocket으로 전송
        messagingTemplate.convertAndSend(
            "/topic/message/update/${message.roomId}",
            messageDto
        )
    }

}