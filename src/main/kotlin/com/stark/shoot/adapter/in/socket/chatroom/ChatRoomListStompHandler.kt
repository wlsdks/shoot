package com.stark.shoot.adapter.`in`.socket.chatroom

import com.stark.shoot.adapter.`in`.socket.dto.chatroom.ChatRoomListRequest
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.GetChatRoomsCommand
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ChatRoomListStompHandler(
    private val findChatRoomUseCase: FindChatRoomUseCase,
    private val messagingTemplate: SimpMessagingTemplate
) {

    /**
     * 채팅방 목록 조회 (WebSocket)
     * 사용자가 속한 채팅방 목록을 조회하여 개인 큐로 전달합니다.
     *
     * WebSocket Endpoint: /rooms
     * Protocol: STOMP over WebSocket
     */
    @MessageMapping("/rooms")
    fun handleChatRoomList(request: ChatRoomListRequest) {
        val command = GetChatRoomsCommand.of(request.userId)
        val rooms = findChatRoomUseCase.getChatRoomsForUser(command)

        messagingTemplate.convertAndSendToUser(
            request.userId.toString(),
            "/queue/rooms",
            rooms
        )
    }

}
