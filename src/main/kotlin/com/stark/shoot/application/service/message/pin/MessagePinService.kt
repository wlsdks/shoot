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

        // 채팅방에 이미 고정된 메시지가 있는지 확인
        val currentPinnedMessage = loadMessagePort.findPinnedMessagesByRoomId(message.roomId, 1).firstOrNull()

        // 도메인 객체의 메서드를 사용하여 메시지 고정 (도메인 규칙 적용)
        val result = message.pinMessageInRoom(userId, currentPinnedMessage)

        // 기존 고정 메시지가 있으면 해제하고 알림 및 이벤트 처리
        result.unpinnedMessage?.let { unpinnedMessage ->
            val savedUnpinnedMessage = saveMessagePort.save(unpinnedMessage)
            sendPinStatusToClients(savedUnpinnedMessage, userId, false)
            publishPinEvent(savedUnpinnedMessage, userId, false)
        }

        // 새로 고정된 메시지 저장
        val savedMessage = saveMessagePort.save(result.pinnedMessage)

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
        val pinEvent = MessagePinEvent.create(
            messageId = message.id ?: return,
            roomId = message.roomId,
            isPinned = isPinned,
            userId = userId
        )

        eventPublisher.publish(pinEvent)
    }

}
