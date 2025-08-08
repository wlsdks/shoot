package com.stark.shoot.adapter.`in`.socket.message

import com.stark.shoot.adapter.`in`.rest.dto.message.DeleteMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.EditMessageRequest
import com.stark.shoot.application.port.`in`.message.DeleteMessageUseCase
import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.DeleteMessageCommand
import com.stark.shoot.application.port.`in`.message.command.EditMessageCommand
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class MessageActionStompHandler(
    private val editMessageUseCase: EditMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase
) {

    /**
     * 메시지 편집 (WebSocket)
     * 메시지 내용을 수정하고 채팅방의 모든 참여자에게 실시간으로 전송합니다.
     *
     * WebSocket Endpoint: /edit
     * Protocol: STOMP over WebSocket
     */
    @MessageMapping("/edit")
    fun handleEditMessage(request: EditMessageRequest) {
        val command = EditMessageCommand.of(request.messageId, request.newContent, request.userId)
        editMessageUseCase.editMessage(command)
    }

    /**
     * 메시지 삭제 (WebSocket)
     * 메시지를 삭제 처리하고 채팅방의 모든 참여자에게 실시간으로 전송합니다.
     *
     * WebSocket Endpoint: /delete
     * Protocol: STOMP over WebSocket
     */
    @MessageMapping("/delete")
    fun handleDeleteMessage(request: DeleteMessageRequest) {
        val command = DeleteMessageCommand.of(request.messageId, request.userId)
        deleteMessageUseCase.deleteMessage(command)
    }

}
