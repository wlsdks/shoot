package com.stark.shoot.adapter.`in`.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatWebSocketHandler : TextWebSocketHandler() {

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.attributes["userId"] as String
        sessions[userId] = session
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // 메시지 처리 로직
    }

    fun sendMessageToUser(userId: String, message: ChatMessage) {
        sessions[userId]?.let { session ->
            session.sendMessage(TextMessage(ObjectMapper().writeValueAsString(message)))
        }
    }

}