package com.stark.shoot.application.service.message.pin

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.pin.MessagePinUseCase
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.event.MessagePinEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

@UseCase
class MessagePinService(
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val eventPublisher: EventPublisher
) : MessagePinUseCase {
    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 고정합니다. (채팅방에는 최대 1개의 고정 메시지만 존재)
     * 만약 이미 고정된 메시지가 있으면, 해당 메시지를 해제하고 새 메시지를 고정합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 고정된 메시지
     */
    override fun pinMessage(
        messageId: String,
        userId: Long
    ): ChatMessage {
        // 메시지 조회
        val message = (loadMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId"))

        // 이미 고정된 메시지인지 확인
        if (message.isPinned) {
            logger.info { "메시지가 이미 고정되어 있습니다: messageId=$messageId" }
            return message
        }

        // 채팅방에 이미 고정된 메시지가 있는지 확인하고, 있다면 해제
        unPinnedAlreadyExistMessage(message, userId)

        // 새 메시지 고정
        val savedMessage = pinnedNewMessage(message, userId)

        // WebSocket을 통해 실시간 업데이트 전송
        sendPinStatusToClients(savedMessage, userId, true)

        // 이벤트 발행
        publishPinEvent(savedMessage, userId, true)
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
        userId: Long
    ): ChatMessage {
        val message = loadMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        // 고정되지 않은 메시지인지 확인
        if (!message.isPinned) {
            logger.info { "메시지가 고정되어 있지 않습니다: messageId=$messageId" }
            return message
        }

        // 도메인 객체의 메서드를 사용하여 메시지 고정 상태 업데이트
        val updatedMessage = message.updatePinStatus(false)

        // 메시지 저장
        val savedMessage = saveMessagePort.save(updatedMessage)

        // WebSocket을 통해 실시간 업데이트 전송
        sendPinStatusToClients(savedMessage, userId, false)

        // 이벤트 발행
        publishPinEvent(savedMessage, userId, false)

        return savedMessage
    }

    /**
     * 채팅방에 이미 고정된 메시지가 있는지 확인하고, 있다면 해제합니다.
     *
     * @param message 메시지
     * @param userId 사용자 ID
     */
    private fun unPinnedAlreadyExistMessage(
        message: ChatMessage,
        userId: Long
    ) {
        // 채팅방에서 고정된 메시지 조회 (최대 1개)
        val currentPinnedMessage = loadMessagePort
            .findPinnedMessagesByRoomId(message.roomId, 1)
            .firstOrNull()

        // 기존 고정 메시지가 있으면 해제
        unPinnedMessage(currentPinnedMessage, userId)
    }

    /**
     * 채팅방에 이미 고정된 메시지가 있는지 확인하고, 있다면 해제합니다.
     *
     * @param currentPinnedMessage 현재 고정된 메시지
     * @param userId 사용자 ID
     */
    private fun unPinnedMessage(
        currentPinnedMessage: ChatMessage?,
        userId: Long
    ) {
        currentPinnedMessage?.let { pinnedMessage ->
            // 도메인 객체의 메서드를 사용하여 이미 고정된 메시지 해제
            val unpinnedMessage = pinnedMessage.updatePinStatus(false)
            val savedUnpinnedMessage = saveMessagePort.save(unpinnedMessage)

            // 고정 해제 이벤트 발행
            sendPinStatusToClients(savedUnpinnedMessage, userId, false)
            publishPinEvent(savedUnpinnedMessage, userId, false)

            logger.info { "기존 고정 메시지 해제: messageId=${pinnedMessage.id}" }
        }
    }

    /**
     * 메시지를 고정합니다.
     *
     * @param message 메시지
     * @param userId 사용자 ID
     * @return 고정된 메시지
     */
    private fun pinnedNewMessage(
        message: ChatMessage,
        userId: Long
    ): ChatMessage {
        // 도메인 객체의 메서드를 사용하여 메시지 고정 상태 업데이트
        val updatedMessage = message.updatePinStatus(true, userId)

        // 메시지 저장
        val savedMessage = saveMessagePort.save(updatedMessage)

        return savedMessage
    }

    /**
     * WebSocket을 통해 메시지 고정 상태 변경을 클라이언트에게 전송
     */
    private fun sendPinStatusToClients(
        message: ChatMessage,
        userId: Long,
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

        webSocketMessageBroker.sendMessage(
            "/topic/pins/$roomId",
            pinStatusData
        )
    }

    /**
     * 메시지 고정 이벤트 발행
     */
    private fun publishPinEvent(
        message: ChatMessage,
        userId: Long,
        isPinned: Boolean
    ) {
        val pinEvent = MessagePinEvent(
            messageId = message.id ?: return,
            roomId = message.roomId,
            isPinned = isPinned,
            userId = userId
        )

        eventPublisher.publish(pinEvent)
    }

}
