package com.stark.shoot.adapter.`in`.rest.socket.message

import com.stark.shoot.adapter.`in`.rest.dto.message.pin.PinMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.reaction.ReactionRequest
import com.stark.shoot.application.port.`in`.message.pin.MessagePinUseCase
import com.stark.shoot.application.port.`in`.message.pin.command.PinMessageCommand
import com.stark.shoot.application.port.`in`.message.reaction.ToggleMessageReactionUseCase
import com.stark.shoot.application.port.`in`.message.reaction.command.ToggleMessageReactionCommand
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class MessageInteractionStompHandler(
    private val toggleMessageReactionUseCase: ToggleMessageReactionUseCase,
    private val messagePinUseCase: MessagePinUseCase
) {

    /**
     * 메시지 반응 토글 (WebSocket)
     * 메시지에 이모지 반응을 추가/제거하고 채팅방의 모든 참여자에게 실시간으로 전송합니다.
     *
     * WebSocket Endpoint: /reaction
     * Protocol: STOMP over WebSocket
     */
    @MessageMapping("/reaction")
    fun handleToggleReaction(request: ReactionRequest) {
        val command = ToggleMessageReactionCommand.of(
            messageId = request.messageId,
            reaction = request.reactionType,  // reactionType 필드 사용
            userId = request.userId
        )
        toggleMessageReactionUseCase.toggleReaction(command)
    }

    /**
     * 메시지 고정 토글 (WebSocket)
     * 메시지가 고정되어 있으면 해제하고, 고정되어 있지 않으면 고정합니다.
     * 모든 참여자에게 실시간으로 전송합니다.
     *
     * WebSocket Endpoint: /pin/toggle
     * Protocol: STOMP over WebSocket
     */
    @MessageMapping("/pin/toggle")
    fun handleTogglePinMessage(request: PinMessageRequest) {
        val command = PinMessageCommand.of(
            messageId = request.messageId,
            userId = request.userId
        )
        messagePinUseCase.pinMessage(command)  // 토글 로직은 UseCase에서 처리
    }

}
