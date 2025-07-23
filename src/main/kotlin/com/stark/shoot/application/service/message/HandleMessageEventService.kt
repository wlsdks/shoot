package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.HandleMessageEventUseCase
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.domain.event.MessageSentEvent
import com.stark.shoot.domain.event.type.EventType
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

@UseCase
class HandleMessageEventService(
    private val saveMessagePort: SaveMessagePort,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val eventPublisher: EventPublisher,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService,
    private val loadUrlContentPort: LoadUrlContentPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val chatMessageMapper: ChatMessageMapper
) : HandleMessageEventUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 저장하고 상태 업데이트를 전송합니다.
     * - 메시지를 저장하고, 채팅방 메타데이터를 업데이트하며, URL 미리보기를 처리합니다.
     * - 상태 업데이트를 웹소켓을 통해 전송합니다.
     */
    override fun handle(event: MessageEvent): Boolean {
        if (event.type != EventType.MESSAGE_CREATED) return false

        val tempId = extractTempId(event) ?: return false
        val roomIdValue = event.data.roomId.value

        sendStatusUpdate(roomIdValue, tempId, MessageStatus.PROCESSING.name, null)

        return try {
            val savedMessage = saveAndMarkMessage(event.data)
            updateChatRoomMetadata(savedMessage)
            publishMessage(savedMessage)
            sendStatusUpdate(roomIdValue, tempId, MessageStatus.SAVED.name, savedMessage.id?.value)
            processChatMessageForUrlPreview(savedMessage)
            true
        } catch (e: Exception) {
            sendErrorResponse(event, e)
            false
        }
    }

    private fun extractTempId(event: MessageEvent): String? {
        return event.data.metadata.tempId ?: run {
            logger.warn { "Received message event without tempId" }
            null
        }
    }

    private fun saveAndMarkMessage(message: ChatMessage): ChatMessage {
        var saved = saveMessagePort.save(message)
        if (saved.readBy[saved.senderId] != true) {
            saved = saveMessagePort.save(saved.markAsRead(saved.senderId))
        }
        saved.id?.let { id ->
            chatRoomCommandPort.updateLastReadMessageId(saved.roomId, saved.senderId, id)
        }
        return saved
    }

    private fun updateChatRoomMetadata(message: ChatMessage) {
        chatRoomQueryPort.findById(message.roomId)?.let { room ->
            val updated = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(room, message)
            chatRoomCommandPort.save(updated)
        }
    }

    private fun publishMessage(message: ChatMessage) {
        webSocketMessageBroker.sendMessage("/topic/messages/${message.roomId.value}", message)
        eventPublisher.publish(MessageSentEvent.create(message))
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

    private fun updateMessageWithPreview(
        message: ChatMessage,
        preview: ChatMessageMetadata.UrlPreview
    ): ChatMessage {
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
