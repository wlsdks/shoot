package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.rest.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.`in`.rest.socket.WebSocketMessageBroker
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
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@UseCase
class HandleMessageEventService(
    private val saveMessagePort: SaveMessagePort,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val loadUrlContentPort: LoadUrlContentPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val chatMessageMapper: ChatMessageMapper,
    private val eventPublisher: EventPublisher,
) : HandleMessageEventUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 저장하고 상태 업데이트를 전송합니다.
     * - 메시지를 저장하고, 채팅방 메타데이터를 업데이트하며, URL 미리보기를 처리합니다.
     * - 상태 업데이트를 웹소켓을 통해 전송합니다.
     */
    @Transactional
    override fun handle(event: MessageEvent): Boolean {
        if (event.type != EventType.MESSAGE_CREATED) return false

        // 메시지 이벤트에서 임시 ID를 추출합니다. (임시 아이디는 웹소켓 상태 표현을 위함)
        val tempId = extractTempId(event) ?: return false
        val roomIdValue = event.data.roomId.value

        // 메시지 상태를 'PROCESSING'으로 업데이트하고 웹소켓을 통해 전송합니다.
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


    /**
     * 메시지를 저장하고 보낸 사람을 읽은 것으로 표시합니다.
     * - 메시지를 저장하고, 보낸 사람의 읽음 상태를 업데이트합니다.
     * - 채팅방의 마지막 읽은 메시지 ID를 업데이트합니다.
     */
    private fun saveAndMarkMessage(message: ChatMessage): ChatMessage {
        var savedMessage = saveMessagePort.save(message)

        // 보낸 사람은 메시지를 읽은 것으로 표시합니다.
        if (savedMessage.readBy[savedMessage.senderId] != true) {
            savedMessage = saveMessagePort.save(savedMessage.markAsRead(savedMessage.senderId))
        }

        // 메시지 ID가 존재하면 채팅방의 마지막 읽은 메시지 ID를 업데이트합니다.
        savedMessage.id?.let { id ->
            chatRoomCommandPort.updateLastReadMessageId(savedMessage.roomId, savedMessage.senderId, id)
        }

        return savedMessage
    }


    /**
     * 채팅방 메타데이터를 업데이트합니다.
     * - 새로운 메시지를 추가하고, 채팅방의 마지막 메시지 ID를 업데이트합니다.
     */
    private fun updateChatRoomMetadata(message: ChatMessage) {
        chatRoomQueryPort.findById(message.roomId)?.let { room ->
            val updated = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(room, message)
            chatRoomCommandPort.save(updated)
        }
    }


    /**
     * 메시지를 웹소켓을 통해 전송하고, 메시지 전송 이벤트를 발행합니다.
     * - 메시지를 특정 채팅방의 토픽으로 전송합니다.
     */
    private fun publishMessage(message: ChatMessage) {
        webSocketMessageBroker.sendMessage("/topic/messages/${message.roomId.value}", message)
        eventPublisher.publish(MessageSentEvent.create(message))
    }


    /**
     * 상태 업데이트를 웹소켓을 통해 전송합니다.
     * - 메시지 상태, 임시 ID, 영구 ID, 오류 메시지를 포함합니다.
     */
    private fun sendStatusUpdate(
        roomId: Long,
        tempId: String,
        status: String,
        persistedId: String?,
        errorMessage: String? = null
    ) {
        val statusUpdate = MessageStatusResponse(
            tempId = tempId,
            status = status,
            persistedId = persistedId,
            errorMessage = errorMessage,
            createdAt = Instant.now().toString()
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


    /**
     * URL 미리보기를 처리합니다.
     * - 메시지 메타데이터에 URL 미리보기가 필요한 경우, URL 콘텐츠를 로드하고 캐시합니다.
     * - URL 미리보기가 성공적으로 로드되면 메시지를 업데이트하고 웹소켓을 통해 전송합니다.
     */
    private fun processChatMessageForUrlPreview(savedMessage: ChatMessage) {
        if (savedMessage.metadata.needsUrlPreview &&
            savedMessage.metadata.previewUrl != null
        ) {
            val previewUrl = savedMessage.metadata.previewUrl ?: return

            try {
                // Jsoup를 사용하여 URL 콘텐츠를 추출합니다.
                val preview = loadUrlContentPort.fetchUrlContent(previewUrl)

                if (preview != null) {
                    // URL 미리보기를 캐시합니다. (로컬 캐시와 Redis에 저장)
                    cacheUrlPreviewPort.cacheUrlPreview(previewUrl, preview)
                    val updatedMessage = updateMessageWithPreview(savedMessage, preview)
                    sendMessageUpdate(updatedMessage)
                }
            } catch (e: Exception) {
                logger.error(e) { "URL 미리보기 처리 실패: $previewUrl" }
            }
        }
    }


    /**
     * 메시지에 URL 미리보기를 추가합니다.
     * - 메시지의 메타데이터에 URL 미리보기를 업데이트합니다.
     */
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

    /**
     * 메시지 업데이트를 웹소켓을 통해 전송합니다.
     * - 메시지의 변경된 내용을 포함하여 특정 채팅방으로 전송합니다.
     */
    private fun sendMessageUpdate(message: ChatMessage) {
        val messageDto = chatMessageMapper.toDto(message)

        webSocketMessageBroker.sendMessage(
            "/topic/message/update/${message.roomId}",
            messageDto
        )
    }

}
