package com.stark.shoot.application.service.message.pin

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.pin.MessagePinUseCase
import com.stark.shoot.application.port.`in`.message.pin.command.PinMessageCommand
import com.stark.shoot.application.port.`in`.message.pin.command.UnpinMessageCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessagePinDomainService
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import java.time.Instant

@UseCase
class MessagePinService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val eventPublisher: EventPublishPort,
    private val messagePinDomainService: MessagePinDomainService
) : MessagePinUseCase {

    /**
     * 메시지를 고정합니다. (채팅방에는 최대 1개의 고정 메시지만 존재)
     * 만약 이미 고정된 메시지가 있으면, 해당 메시지를 해제하고 새 메시지를 고정합니다.
     *
     * @param command 메시지 고정 커맨드
     * @return 고정된 메시지
     */
    override fun pinMessage(command: PinMessageCommand): ChatMessage {
        // 메시지 조회
        val message = (messageQueryPort.findById(command.messageId))
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=${command.messageId}")

        // 채팅방에 이미 고정된 메시지가 있는지 확인
        val currentPinnedMessage = messageQueryPort.findPinnedMessagesByRoomId(message.roomId, 1).firstOrNull()

        // 도메인 객체의 메서드를 사용하여 메시지 고정 (도메인 규칙 적용)
        val result = message.pinMessageInRoom(command.userId, currentPinnedMessage)

        // 기존 고정 메시지가 있으면 해제하고 알림 및 이벤트 처리
        result.unpinnedMessage?.let { unpinnedMessage ->
            val savedUnpinnedMessage = messageCommandPort.save(unpinnedMessage)
            sendPinStatusToClients(savedUnpinnedMessage, command.userId, false)
            publishPinEvent(savedUnpinnedMessage, command.userId, false)
        }

        // 새로 고정된 메시지 저장
        val savedMessage = messageCommandPort.save(result.pinnedMessage)

        // WebSocket을 통해 실시간 업데이트 전송
        sendPinStatusToClients(savedMessage, command.userId, true)

        // 이벤트 발행
        publishPinEvent(savedMessage, command.userId, true)

        return savedMessage
    }

    /**
     * 메시지 고정을 해제합니다.
     *
     * @param command 메시지 고정 해제 커맨드
     * @return 고정 해제된 메시지
     */
    override fun unpinMessage(command: UnpinMessageCommand): ChatMessage {
        val message = messageQueryPort.findById(command.messageId)
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=${command.messageId}")

        // 고정되지 않은 메시지인지 확인
        if (!message.isPinned) {
            return message
        }

        // 도메인 객체의 메서드를 사용하여 메시지 고정 상태 업데이트
        message.updatePinStatus(false)

        // 메시지 저장
        val savedMessage = messageCommandPort.save(message)

        // WebSocket을 통해 실시간 업데이트 전송
        sendPinStatusToClients(savedMessage, command.userId, false)

        // 이벤트 발행
        publishPinEvent(savedMessage, command.userId, false)

        return savedMessage
    }


    /**
     * WebSocket을 통해 메시지 고정 상태 변경을 클라이언트에게 전송
     */
    private fun sendPinStatusToClients(
        message: ChatMessage,
        userId: UserId,
        isPinned: Boolean
    ) {
        val roomId = message.roomId.value
        val messageId = message.id?.value ?: return

        // 채팅방의 모든 클라이언트에게 메시지 고정 상태 변경을 전송
        val pinStatusData = mapOf(
            "messageId" to messageId,
            "roomId" to roomId,
            "userId" to userId.value,
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
        userId: UserId,
        isPinned: Boolean
    ) {
        val pinEvent = messagePinDomainService.createPinEvent(message, userId, isPinned)

        if (pinEvent != null) {
            eventPublisher.publishEvent(pinEvent)
        }
    }

}
