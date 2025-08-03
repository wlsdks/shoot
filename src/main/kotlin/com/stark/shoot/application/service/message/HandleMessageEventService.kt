package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.HandleMessageEventUseCase
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.MessageStatusNotificationPort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.domain.event.MessageSentEvent
import com.stark.shoot.domain.event.type.EventType
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional

@UseCase
class HandleMessageEventService(
    private val saveMessagePort: SaveMessagePort,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val loadUrlContentPort: LoadUrlContentPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService,
    private val messageStatusNotificationPort: MessageStatusNotificationPort,
    private val eventPublisher: EventPublisher,
) : HandleMessageEventUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 저장하고 상태 업데이트를 전송합니다.
     */
    @Transactional
    override fun handle(event: MessageEvent): Boolean {
        if (event.type != EventType.MESSAGE_CREATED) return false

        val message = event.data
        val tempId = message.metadata.tempId

        return try {
            throw IllegalArgumentException("HandleMessageEventService는 MESSAGE_CREATED 이벤트만 처리합니다. 현재 이벤트 타입: ${event.type}")

            // 메시지 저장 및 메타데이터 업데이트
            saveMessageAndUpdateMetadata(message)
            // URL 미리보기 처리 (백그라운드)
            processUrlPreviewIfNeeded(message)
            true
        } catch (e: Exception) {
            logger.error(e) { "메시지 영속화 실패: messageId=${message.id?.value}" }
            notifyPersistenceFailure(message, tempId, e)
            false
        }
    }

    /**
     * 영속화 실패를 사용자에게 알립니다.
     */
    private fun notifyPersistenceFailure(
        message: ChatMessage,
        tempId: String?,
        exception: Exception
    ) {
        if (tempId.isNullOrEmpty()) {
            logger.warn { "tempId가 없어서 영속화 실패 알림을 보낼 수 없음: messageId=${message.id?.value}" }
            return
        }

        messageStatusNotificationPort.notifyMessageStatus(
            roomId = message.roomId.value,
            tempId = tempId,
            status = MessageStatus.FAILED,
            errorMessage = "영속화 실패: ${exception.message}"
        )

        logger.warn { "영속화 실패 알림 전송: roomId=${message.roomId.value}, tempId=$tempId" }
    }

    /**
     * 메시지 저장 및 메타데이터 업데이트
     */
    private fun saveMessageAndUpdateMetadata(message: ChatMessage) {
        // 1. 메시지 저장 및 읽음 처리
        val savedMessage = saveAndMarkMessage(message)

        // 2. 채팅방 메타데이터 업데이트
        updateChatRoomMetadata(savedMessage)

        // 3. 도메인 이벤트 발행
        eventPublisher.publish(MessageSentEvent.create(savedMessage))
    }

    /**
     * 메시지를 저장하고 보낸 사람을 읽은 것으로 표시합니다.
     */
    private fun saveAndMarkMessage(message: ChatMessage): ChatMessage {
        var savedMessage = saveMessagePort.save(message)

        // 보낸 사람은 메시지를 읽은 것으로 표시
        if (savedMessage.readBy[savedMessage.senderId] != true) {
            savedMessage = saveMessagePort.save(savedMessage.markAsRead(savedMessage.senderId))
        }

        // 채팅방의 마지막 읽은 메시지 ID 업데이트
        savedMessage.id?.let { id ->
            chatRoomCommandPort.updateLastReadMessageId(savedMessage.roomId, savedMessage.senderId, id)
        }

        return savedMessage
    }

    /**
     * 채팅방 메타데이터를 업데이트합니다.
     */
    private fun updateChatRoomMetadata(message: ChatMessage) {
        chatRoomQueryPort.findById(message.roomId)?.let { room ->
            val updated = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(room, message)
            chatRoomCommandPort.save(updated)
        }
    }

    /**
     * URL 미리보기 처리 (필요시)
     */
    private fun processUrlPreviewIfNeeded(message: ChatMessage) {
        if (message.metadata.needsUrlPreview && message.metadata.previewUrl != null) {
            try {
                val previewUrl = message.metadata.previewUrl!!
                val preview = loadUrlContentPort.fetchUrlContent(previewUrl)

                if (preview != null) {
                    cacheUrlPreviewPort.cacheUrlPreview(previewUrl, preview)
                    // URL 미리보기 업데이트는 별도 이벤트로 처리
                }
            } catch (e: Exception) {
                logger.error(e) { "URL 미리보기 처리 실패: ${message.metadata.previewUrl}" }
            }
        }
    }

}
