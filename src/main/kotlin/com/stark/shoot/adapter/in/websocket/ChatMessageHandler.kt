package com.stark.shoot.adapter.`in`.websocket

import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import com.stark.shoot.application.port.`in`.SendMessageUseCase
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class ChatMessageHandler(
    private val sendMessageUseCase: SendMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate
) {

    /**
     * 클라이언트가 `/app/chat`로 메시지를 전송하면 호출됩니다.
     * 메시지를 저장한 후, `/topic/messages/{roomId}` 경로로 브로드캐스트합니다.
     *
     * @param message 클라이언트로부터 받은 메시지 요청 객체.
     */
    @MessageMapping("/chat")
    fun handleChatMessage(message: ChatMessageRequest, principal: Principal) {
        println("Received message: ${message.content}")

        val userId = principal.name // 현재 사용자 ID 가져오기
//        if(!hasPermissionToSendMessage(userId, message.roomId)) {
        // 권한이 없는 경우
//            throw AccessDeniedException("You don't have permission to send message to this room.")
//        }

        // 메시지 저장
        val savedMessage = sendMessageUseCase.sendMessage(
            roomId = message.roomId,
            senderId = message.senderId,
            messageContent = message.toDomain()
        )

        // 브로드캐스트
        messagingTemplate.convertAndSend("/topic/messages/${message.roomId}", savedMessage)
    }

//    private fun hasPermissionToSendMessage(userId: String, roomId: String): Boolean {
//        val chatRoom = loadChatRoomPort.findById(roomId.toObjectId()) ?: return false
//        return chatRoom.participants.contains(userId)
//    }


}