package com.stark.shoot.application.service.message.pin

import com.stark.shoot.application.port.`in`.message.pin.MessagePinUseCase
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.LoadChatMessagePort
import com.stark.shoot.application.port.out.message.SaveChatMessagePort
import com.stark.shoot.domain.chat.event.MessagePinEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.common.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MessagePinService(
    private val loadChatMessagePort: LoadChatMessagePort,
    private val saveChatMessagePort: SaveChatMessagePort,
    private val messagingTemplate: SimpMessagingTemplate,
    private val eventPublisher: EventPublisher
) : MessagePinUseCase {
    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 고정합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 고정된 메시지
     */
    override fun pinMessage(
        messageId: String,
        userId: String
    ): ChatMessage {
        val message = (loadChatMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId"))

        // 이미 고정된 메시지인지 확인
        if (message.isPinned) {
            logger.info { "메시지가 이미 고정되어 있습니다: messageId=$messageId" }
            return message
        }

        // 채팅방에 이미 고정된 메시지가 있는지 확인하고, 있다면 해제
        val roomId = message.roomId
        val currentPinnedMessage = loadChatMessagePort.findPinnedMessagesByRoomId(roomId.toObjectId(), 1).firstOrNull()

        // 기존 고정 메시지가 있으면 해제
        currentPinnedMessage?.let { pinnedMessage ->
            // 이미 고정된 메시지 해제
            val unpinnedMessage = pinnedMessage.copy(
                isPinned = false,
                pinnedBy = null,
                pinnedAt = null,
                updatedAt = Instant.now()
            )
            val savedUnpinnedMessage = saveChatMessagePort.save(unpinnedMessage)

            // 고정 해제 이벤트 발행
            sendPinStatusToClients(savedUnpinnedMessage, userId, false)
            publishPinEvent(savedUnpinnedMessage, userId, false)

            logger.info { "기존 고정 메시지 해제: messageId=${pinnedMessage.id}" }
        }

        // 새 메시지 고정
        val updatedMessage = message.copy(
            isPinned = true,
            pinnedBy = userId,
            pinnedAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // 메시지 저장
        val savedMessage = saveChatMessagePort.save(updatedMessage)

        // WebSocket을 통해 실시간 업데이트 전송
        sendPinStatusToClients(savedMessage, userId, true)

        // 이벤트 발행
        publishPinEvent(savedMessage, userId, true)

        logger.info { "새 메시지가 고정되었습니다: messageId=$messageId, userId=$userId, roomId=$roomId" }
        return savedMessage
    }

    /**
     * 메시지 고정을 해제합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 고정 해제된 메시지
     */
    override fun unpinMessage(
        messageId: String,
        userId: String
    ): ChatMessage {
        val message = loadChatMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        // 고정되지 않은 메시지인지 확인
        if (!message.isPinned) {
            logger.info { "메시지가 고정되어 있지 않습니다: messageId=$messageId" }
            return message
        }

        // 메시지 업데이트
        val updatedMessage = message.copy(
            isPinned = false,
            pinnedBy = null,
            pinnedAt = null,
            updatedAt = Instant.now()
        )

        // 메시지 저장
        val savedMessage = saveChatMessagePort.save(updatedMessage)

        // WebSocket을 통해 실시간 업데이트 전송
        sendPinStatusToClients(savedMessage, userId, false)

        // 이벤트 발행
        publishPinEvent(savedMessage, userId, false)

        logger.info { "메시지 고정이 해제되었습니다: messageId=$messageId, userId=$userId" }
        return savedMessage
    }

    /**
     * 채팅방에서 고정된 메시지를 조회합니다.
     * 한 채팅방에는 최대 1개의 고정 메시지만 존재합니다.
     *
     * @param roomId 채팅방 ID
     * @return 고정된 메시지 목록 (최대 1개)
     */
    override fun getPinnedMessages(
        roomId: String
    ): List<ChatMessage> {
        logger.debug { "채팅방의 고정된 메시지 조회: roomId=$roomId" }

        // 채팅방에서 고정된 메시지 조회 (최대 1개)
        val pinnedMessages = loadChatMessagePort.findPinnedMessagesByRoomId(roomId.toObjectId(), 1)

        logger.debug { "고정된 메시지 ${pinnedMessages.size}개 조회됨: roomId=$roomId" }
        return pinnedMessages
    }

    /**
     * WebSocket을 통해 메시지 고정 상태 변경을 클라이언트에게 전송
     */
    private fun sendPinStatusToClients(
        message: ChatMessage,
        userId: String,
        isPinned: Boolean
    ) {
        val roomId = message.roomId
        val messageId = message.id ?: return

        // 채팅방의 모든 클라이언트에게 메시지 고정 상태 변경을 전송
        val pinStatusData = mapOf(
            "messageId" to messageId,
            "roomId" to roomId,
            "userId" to userId,
            "isPinned" to isPinned,
            "timestamp" to Instant.now().toString()
        )

        messagingTemplate.convertAndSend("/topic/pins/$roomId", pinStatusData)
        logger.debug { "WebSocket을 통해 메시지 고정 상태 변경 전송: messageId=$messageId, isPinned=$isPinned" }
    }

    /**
     * 메시지 고정 이벤트 발행
     */
    private fun publishPinEvent(
        message: ChatMessage,
        userId: String,
        isPinned: Boolean
    ) {
        val messageId = message.id ?: return

        val pinEvent = MessagePinEvent(
            messageId = messageId,
            roomId = message.roomId,
            isPinned = isPinned,
            userId = userId
        )

        eventPublisher.publish(pinEvent)
        logger.debug { "메시지 고정 이벤트 발행: messageId=$messageId, isPinned=$isPinned" }
    }

}