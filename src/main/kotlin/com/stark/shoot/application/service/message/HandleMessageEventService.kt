package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.HandleMessageEventUseCase
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.message.MessageStatusNotificationPort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.event.MentionEvent
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
    private val messageStatusNotificationPort: MessageStatusNotificationPort,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService,
    private val eventPublisher: EventPublishPort,
    private val userQueryPort: com.stark.shoot.application.port.out.user.UserQueryPort,
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
            // 메시지 저장 및 메타데이터 업데이트
            saveMessageAndUpdateMetadata(message)

            // URL 미리보기 처리 (백그라운드)
            processUrlPreviewIfNeeded(message)

            // 성공 시 상태 업데이트 전송
            notifyPersistenceSuccess(message, tempId)
            true
        } catch (e: Exception) {
            logger.error(e) { "메시지 영속화 실패: messageId=${message.id?.value}" }
            // 실패 시 사용자에게 알림
            notifyPersistenceFailure(message, tempId, e)
            false
        }
    }

    /**
     * 영속화 성공을 사용자에게 알립니다.
     */
    private fun notifyPersistenceSuccess(
        message: ChatMessage,
        tempId: String?
    ) {
        if (tempId.isNullOrEmpty()) {
            logger.debug { "tempId가 없어서 영속화 성공 알림을 건너뜀: messageId=${message.id?.value}" }
            return
        }

        messageStatusNotificationPort.notifyMessageStatus(
            roomId = message.roomId.value,
            tempId = tempId,
            status = MessageStatus.SENT,
            errorMessage = null
        )

        logger.debug { "영속화 성공 알림 전송: roomId=${message.roomId.value}, tempId=$tempId" }
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

        // 3. 도메인 이벤트 발행 (MongoDB 저장 후 발행으로 데이터 일관성 보장)
        publishDomainEvents(savedMessage)
    }

    /**
     * 메시지를 저장하고 보낸 사람을 읽은 것으로 표시합니다.
     * 성능 최적화: 저장 전에 markAsRead를 호출하여 단일 저장으로 개선
     */
    private fun saveAndMarkMessage(message: ChatMessage): ChatMessage {
        // 저장 전에 발신자를 읽음 처리 (이중 저장 방지)
        if (message.readBy[message.senderId] != true) {
            message.markAsRead(message.senderId)
        }

        // 단일 저장으로 MongoDB 쓰기 부하 50% 감소
        val savedMessage = saveMessagePort.save(message)

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
        val previewUrl = message.metadata.previewUrl
        if (message.metadata.needsUrlPreview && previewUrl != null) {
            try {
                val preview = loadUrlContentPort.fetchUrlContent(previewUrl)

                if (preview != null) {
                    cacheUrlPreviewPort.cacheUrlPreview(previewUrl, preview)
                    // URL 미리보기 업데이트는 별도 이벤트로 처리
                }
            } catch (e: Exception) {
                logger.error(e) { "URL 미리보기 처리 실패: $previewUrl" }
            }
        }
    }

    /**
     * 도메인 이벤트를 발행합니다.
     * MessageSentEvent와 필요시 MentionEvent를 발행합니다.
     * MongoDB 저장 후 발행하므로 데이터 일관성이 보장됩니다.
     */
    private fun publishDomainEvents(message: ChatMessage) {
        try {
            // 1. MessageSentEvent 발행
            val messageSentEvent = MessageSentEvent.create(message)
            eventPublisher.publishEvent(messageSentEvent)

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

        eventPublisher.publishEvent(mentionEvent)
        logger.debug { "MentionEvent published for ${mentionedUsers.size} users" }
    }

}
