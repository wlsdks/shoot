package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.ProcessMessageCommand
import com.stark.shoot.application.port.`in`.message.consume.ConsumeMessageEventUseCase
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.domain.event.type.EventType
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

@UseCase
class ConsumeMessageEventService(
    private val processMessageUseCase: ProcessMessageUseCase,
    private val loadUrlContentPort: LoadUrlContentPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val chatMessageMapper: ChatMessageMapper
) : ConsumeMessageEventUseCase {

    private val logger = KotlinLogging.logger {}

    override fun consume(event: MessageEvent): Boolean {
        if (event.type != EventType.MESSAGE_CREATED) return false

        return try {
            val tempId = event.data.metadata.tempId ?: run {
                logger.warn { "Received message event without tempId" }
                return false
            }
            val roomId = event.data.roomId

            // 상태: PROCESSING
            sendStatusUpdate(roomId.value, tempId, MessageStatus.PROCESSING.name, null)

            // 메시지 저장
            val command = ProcessMessageCommand.of(event.data)
            val savedMessage = processMessageUseCase.processMessageCreate(command)

            // 상태: SAVED
            sendStatusUpdate(roomId.value, tempId, MessageStatus.SAVED.name, savedMessage.id?.value)

            // URL 미리보기 처리
            processChatMessageForUrlPreview(savedMessage)

            true
        } catch (e: Exception) {
            sendErrorResponse(event, e)
            false
        }
    }

    private fun sendStatusUpdate(
        roomId: Long,
        tempId: String,
        status: String,
        persistedId: String?,
        errorMessage: String? = null
    ) {
        val createdAt = Instant.now().toString()
        val statusUpdate = MessageStatusResponse(
            tempId = tempId,
            status = status,
            persistedId = persistedId,
            errorMessage = errorMessage,
            createdAt = createdAt
        )
        webSocketMessageBroker.sendMessage("/topic/message/status/$roomId", statusUpdate)
    }

    private fun sendErrorResponse(event: MessageEvent, e: Exception) {
        val tempId = event.data.metadata.tempId
        if (tempId != null) {
            sendStatusUpdate(
                event.data.roomId.value,
                tempId,
                MessageStatus.FAILED.name,
                null,
                e.message
            )
        }
        logger.error(e) { "메시지 처리 오류: ${e.message}" }
    }

    private fun processChatMessageForUrlPreview(savedMessage: ChatMessage) {
        if (savedMessage.metadata.needsUrlPreview && savedMessage.metadata.previewUrl != null) {
            val previewUrl = savedMessage.metadata.previewUrl ?: return
            try {
                val preview = loadUrlContentPort.fetchUrlContent(previewUrl)
                if (preview != null) {
                    cacheUrlPreviewPort.cacheUrlPreview(previewUrl, preview)
                    val updatedMessage = updateMessageWithPreview(savedMessage, preview)
                    sendMessageUpdate(updatedMessage)
                }
            } catch (e: Exception) {
                logger.error(e) { "URL 미리보기 처리 실패: $previewUrl" }
            }
        }
    }

    private fun updateMessageWithPreview(message: ChatMessage, preview: ChatMessageMetadata.UrlPreview): ChatMessage {
        val updatedMetadata = message.metadata
        val currentContent = message.content
        val currentMetadata = currentContent.metadata ?: ChatMessageMetadata()
        val updatedContentMetadata = currentMetadata.copy(urlPreview = preview)
        val updatedContent = currentContent.copy(metadata = updatedContentMetadata)
        return message.copy(
            content = updatedContent,
            metadata = updatedMetadata
        )
    }

    private fun sendMessageUpdate(message: ChatMessage) {
        val messageDto = chatMessageMapper.toDto(message)
        webSocketMessageBroker.sendMessage(
            "/topic/message/update/${message.roomId}",
            messageDto
        )
    }
}
