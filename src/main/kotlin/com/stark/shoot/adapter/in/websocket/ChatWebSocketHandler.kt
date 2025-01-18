package com.stark.shoot.adapter.`in`.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import com.stark.shoot.application.port.`in`.SendMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket 기반의 채팅 메시지 처리를 위한 핸들러 클래스.
 * 클라이언트와의 WebSocket 연결을 관리하며, 메시지를 수신하고 처리 후 저장 및 브로드캐스트합니다.
 *
 * @property sendMessageUseCase 메시지를 저장하고 비즈니스 로직을 처리하는 유스케이스.
 */
@Component
class ChatWebSocketHandler(
    private val sendMessageUseCase: SendMessageUseCase
) : TextWebSocketHandler() {

    // 사용자 ID와 WebSocketSession 간의 매핑을 저장하는 데이터 구조.
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    /**
     * WebSocket 연결이 성공적으로 설정된 경우 호출됩니다.
     * 사용자 ID를 세션과 매핑하여 WebSocket 연결을 관리합니다.
     *
     * @param session 연결된 클라이언트의 WebSocketSession.
     */
    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.attributes["userId"] as String
        sessions[userId] = session
    }

    /**
     * WebSocket으로부터 텍스트 메시지를 수신했을 때 호출됩니다.
     * 메시지를 파싱하고 저장한 뒤, 연결된 클라이언트들에게 브로드캐스트합니다.
     *
     * @param session 메시지를 보낸 클라이언트의 WebSocketSession.
     * @param message 클라이언트로부터 받은 메시지 (JSON 형식의 TextMessage).
     */
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // JSON 메시지를 ChatMessageRequest 객체로 변환
        val chatMessage = ObjectMapper().readValue(message.payload, ChatMessageRequest::class.java)

        // 메시지 저장 및 비즈니스 로직 처리
        val savedMessage = sendMessageUseCase.sendMessage(
            roomId = chatMessage.roomId,
            senderId = chatMessage.senderId,
            messageContent = chatMessage.toDomain()
        )

        // 해당 채팅방의 사용자들에게 메시지 브로드캐스트
        broadcastMessage(chatMessage.roomId, savedMessage)
    }

    /**
     * 특정 채팅방에 연결된 모든 사용자에게 메시지를 전송합니다.
     *
     * @param roomId 메시지를 보낼 채팅방 ID.
     * @param message 전송할 메시지.
     */
    private fun broadcastMessage(roomId: String, message: ChatMessage) {
        sessions.values.forEach { session ->
            session.sendMessage(TextMessage(ObjectMapper().writeValueAsString(message)))
        }
    }

}
