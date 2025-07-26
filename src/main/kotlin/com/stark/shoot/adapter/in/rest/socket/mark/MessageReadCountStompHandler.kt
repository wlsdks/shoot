package com.stark.shoot.adapter.`in`.rest.socket.mark

import com.stark.shoot.adapter.`in`.rest.dto.message.read.ChatReadRequest
import com.stark.shoot.application.port.`in`.message.mark.MessageReadUseCase
import com.stark.shoot.application.port.`in`.message.mark.command.MarkAllMessagesAsReadCommand
import com.stark.shoot.application.port.`in`.message.mark.command.MarkMessageAsReadCommand
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class MessageReadCountStompHandler(
    private val messageReadUseCase: MessageReadUseCase
) {

    @Operation(
        summary = "전체 메시지 읽음 처리 (WebSocket)",
        description = """
            - 채팅방의 모든 메시지를 읽음 처리합니다.
            - WebSocket을 통해 실시간으로 읽음 상태를 업데이트합니다.
        """
    )
    @MessageMapping("/read-all")
    fun handleReadAll(request: ChatReadRequest) {
        val command = MarkAllMessagesAsReadCommand.of(
            roomId = request.roomId,
            userId = request.userId,
            requestId = request.requestId
        )
        messageReadUseCase.markAllMessagesAsRead(command)
    }

    @Operation(
        summary = "단일 메시지 읽음 처리 (WebSocket)",
        description = """
            - 특정 메시지를 읽음 처리합니다.
            - WebSocket을 통해 실시간으로 읽음 상태를 업데이트합니다.
        """
    )
    @MessageMapping("/read")
    fun handleRead(request: ChatReadRequest) {
        request.messageId?.let { messageId ->
            val command = MarkMessageAsReadCommand.of(
                messageId = messageId,
                userId = request.userId
            )
            messageReadUseCase.markMessageAsRead(command)
        }
    }

}
