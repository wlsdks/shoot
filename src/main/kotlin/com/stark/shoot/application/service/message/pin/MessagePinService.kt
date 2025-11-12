package com.stark.shoot.application.service.message.pin

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.pin.MessagePinUseCase
import com.stark.shoot.application.port.`in`.message.pin.command.PinMessageCommand
import com.stark.shoot.application.port.`in`.message.pin.command.UnpinMessageCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.pin.MessagePinCommandPort
import com.stark.shoot.application.port.out.message.pin.MessagePinQueryPort
import com.stark.shoot.application.acl.*
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessagePinDomainService
import com.stark.shoot.domain.chat.pin.MessagePin
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.UnauthorizedException
import com.stark.shoot.domain.chatroom.constants.ChatRoomConstants
import com.stark.shoot.infrastructure.util.orThrowNotFound
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@UseCase
class MessagePinService(
    private val messageQueryPort: MessageQueryPort,
    private val messagePinCommandPort: MessagePinCommandPort,
    private val messagePinQueryPort: MessagePinQueryPort,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val eventPublisher: EventPublishPort,
    private val messagePinDomainService: MessagePinDomainService,
    private val chatRoomConstants: ChatRoomConstants
) : MessagePinUseCase {

    /**
     * 메시지를 고정합니다. (채팅방에는 최대 N개의 고정 메시지 존재 가능)
     * 최대 개수를 초과하면 예외를 발생시킵니다.
     *
     * @param command 메시지 고정 커맨드
     * @return 고정된 메시지
     * @throws IllegalStateException 최대 고정 개수를 초과하는 경우
     */
    override fun pinMessage(command: PinMessageCommand): ChatMessage {
        val maxPinnedMessages = chatRoomConstants.maxPinnedMessages

        // 메시지 조회
        val message = messageQueryPort.findById(command.messageId)
            .orThrowNotFound("메시지", command.messageId)

        // 권한 검증: 채팅방 참여자만 메시지를 고정할 수 있음
        val chatRoomId = com.stark.shoot.domain.chatroom.vo.ChatRoomId.from(message.roomId.value)
        val chatRoom = chatRoomQueryPort.findById(chatRoomId)
            ?: throw IllegalStateException("채팅방을 찾을 수 없습니다: ${message.roomId}")

        if (command.userId !in chatRoom.participants) {
            throw UnauthorizedException("채팅방 참여자만 메시지를 고정할 수 있습니다.")
        }

        // 이미 고정된 메시지인지 확인 (MessagePin Aggregate 조회)
        val existingPin = messagePinQueryPort.findByMessageId(command.messageId)
        if (existingPin != null) {
            // 이미 고정된 메시지이므로 그대로 반환
            return message
        }

        // 채팅방에 이미 고정된 메시지 개수 확인
        val currentPinnedCount = messagePinQueryPort.countByRoomId(message.roomId)

        // 최대 고정 개수 확인
        if (currentPinnedCount >= maxPinnedMessages) {
            throw IllegalStateException(
                "최대 고정 메시지 개수를 초과했습니다. (현재: $currentPinnedCount, 최대: $maxPinnedMessages)"
            )
        }

        // MessagePin Aggregate 생성 및 저장
        val messagePin = MessagePin.create(
            messageId = command.messageId,
            roomId = message.roomId,
            pinnedBy = command.userId
        )
        messagePinCommandPort.save(messagePin)

        // WebSocket을 통해 실시간 업데이트 전송
        sendPinStatusToClients(message, command.userId, true)

        // 이벤트 발행
        publishPinEvent(message, command.userId, true)

        return message
    }

    /**
     * 메시지 고정을 해제합니다.
     *
     * @param command 메시지 고정 해제 커맨드
     * @return 고정 해제된 메시지
     */
    override fun unpinMessage(command: UnpinMessageCommand): ChatMessage {
        val message = messageQueryPort.findById(command.messageId)
            .orThrowNotFound("메시지", command.messageId)

        // 권한 검증: 채팅방 참여자만 메시지 고정을 해제할 수 있음
        val chatRoomId = com.stark.shoot.domain.chatroom.vo.ChatRoomId.from(message.roomId.value)
        val chatRoom = chatRoomQueryPort.findById(chatRoomId)
            ?: throw IllegalStateException("채팅방을 찾을 수 없습니다: ${message.roomId}")

        if (command.userId !in chatRoom.participants) {
            throw UnauthorizedException("채팅방 참여자만 메시지 고정을 해제할 수 있습니다.")
        }

        // MessagePin Aggregate 조회
        val messagePin = messagePinQueryPort.findByMessageId(command.messageId)

        // 고정되지 않은 메시지인지 확인
        if (messagePin == null) {
            return message
        }

        // MessagePin Aggregate 삭제
        messagePinCommandPort.deleteByMessageId(command.messageId)

        // WebSocket을 통해 실시간 업데이트 전송
        sendPinStatusToClients(message, command.userId, false)

        // 이벤트 발행
        publishPinEvent(message, command.userId, false)

        return message
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
