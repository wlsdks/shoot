package com.stark.shoot.adapter.`in`.web.socket.mark

import com.stark.shoot.adapter.`in`.web.dto.message.read.ChatReadRequest
import com.stark.shoot.application.port.`in`.message.mark.MessageReadUseCase
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId
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
        messageReadUseCase.markAllMessagesAsRead(
            roomId = ChatRoomId.from(request.roomId),
            userId = UserId.from(request.userId),
            requestId = request.requestId
        )
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
            messageReadUseCase.markMessageAsRead(
                messageId = MessageId.from(messageId),
                userId = UserId.from(request.userId)
            )
        }
    }

}
