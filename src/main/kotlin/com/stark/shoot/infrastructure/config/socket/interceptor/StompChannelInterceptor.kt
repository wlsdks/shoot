package com.stark.shoot.infrastructure.config.socket.interceptor

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.infrastructure.common.exception.web.WebSocketException
import com.stark.shoot.infrastructure.config.socket.StompPrincipal
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.core.Authentication

/**
 * StompChannelInterceptor는 STOMP 프로토콜 기반의 메시지를 가로채어
 * 경로 접근 권한을 확인하고, 필요 시 예외를 발생시켜 메시지 처리를 중단하는 역할을 담당합니다.
 *
 * 메시지가 인바운드 채널(서버로 들어오는 경로)을 통과할 때 preSend()가 호출되어
 * STOMP 명령어(SEND, SUBSCRIBE 등)를 확인하고 권한 로직을 수행할 수 있습니다.
 */
class StompChannelInterceptor(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val findUserPort: FindUserPort,
    private val objectMapper: ObjectMapper
) : ChannelInterceptor {

    private val logger = KotlinLogging.logger {}

    /**
     * STOMP 프레임이 인바운드 채널을 통과하기 전에 호출됩니다.
     * 메시지의 STOMP 명령어와 헤더를 확인하여 특정 경로의 접근 권한을 검사할 수 있습니다.
     * HandshakeInterceptor에서 저장했던 인증 객체를, 실제 메시지 처리가 시작되기 전에 StompHeaderAccessor의 user 필드(Principal)에 주입하게 됩니다.
     * 그 결과, 컨트롤러의 STOMP 핸들러 메서드(@MessageMapping)에서 principal 파라미터를 받을 수 있게 됩니다.
     */
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = StompHeaderAccessor.wrap(message)
        val command = accessor.command ?: return message
        val destination = accessor.destination

        // SockJS fallback 경로는 인증 생략
        val path = accessor.sessionAttributes?.get("sockJsPath") as? String ?: ""
        if (path.contains("/xhr_send") || path.contains("/xhr_streaming") || path.endsWith("/info")) {
            return message
        }

        // 인증 정보 확인 및 복구
        if (accessor.user == null) {
            val auth = accessor.sessionAttributes?.get("authentication") as? Authentication
            if (auth != null) {
                accessor.user = StompPrincipal(auth.name)
                logger.info { "Restored authentication for user: ${auth.name}" }
            } else {
                logger.error { "No authentication found for message: $destination" }
                throw WebSocketException("Authentication required")
            }
        }

        // SEND 명령어 처리
        if (command == StompCommand.SEND) {
            when (destination) {
                "/app/chat" -> {
                    val parsedMessage = getChatMessage(message) ?: return message
                    validateMessage(parsedMessage)
                }

                "/app/active" -> {
                    // active 메시지는 파싱만 확인 (필요 시 별도 모델로 파싱)
                    getRawPayload(message)
                }

                "/app/typing" -> {
                    // typing 메시지는 파싱만 확인 (필요 시 별도 모델로 파싱)
                    getRawPayload(message)
                }

                else -> {
                    logger.warn { "Unknown destination: $destination" }
                }
            }
        }
        return message
    }

    private fun getChatMessage(message: Message<*>): ChatMessageRequest? {
        val payload = getRawPayload(message) ?: return null
        return try {
            objectMapper.readValue(payload, ChatMessageRequest::class.java)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse chat message" }
            throw WebSocketException("Failed to parse chat message")
        }
    }


    private fun getRawPayload(message: Message<*>): String? {
        val payload = message.payload
        if (payload == null || (payload is String && payload.trim().isEmpty())) {
            return null
        }
        return when (payload) {
            is String -> payload
            is ByteArray -> String(payload)
            else -> throw WebSocketException("Unsupported message payload type")
        }
    }


    /**
     * ChatMessageRequest 유효성 검사
     */
    private fun validateMessage(message: ChatMessageRequest) {
        if (message.roomId.isBlank()) {
            throw WebSocketException("Room ID is required")
        }
        if (message.content.text.length > 1000) {  // 예시: 1000자 제한
            throw WebSocketException("Message content too long")
        }
    }

}
