package com.stark.shoot.infrastructure.config.socket.interceptor

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.infrastructure.common.exception.WebSocketException
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
    private val retrieveUserPort: RetrieveUserPort,
    private val objectMapper: ObjectMapper
) : ChannelInterceptor {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val TOPIC_PREFIX = "/topic/messages/"
        private const val QUEUE_PREFIX = "/queue/messages/"
    }

    /**
     * STOMP 프레임이 인바운드 채널을 통과하기 전에 호출됩니다.
     * 메시지의 STOMP 명령어와 헤더를 확인하여 특정 경로의 접근 권한을 검사할 수 있습니다.
     * HandshakeInterceptor에서 저장했던 인증 객체를, 실제 메시지 처리가 시작되기 전에 StompHeaderAccessor의 user 필드(Principal)에 주입하게 됩니다.
     * 그 결과, 컨트롤러의 STOMP 핸들러 메서드(@MessageMapping)에서 principal 파라미터를 받을 수 있게 됩니다.
     */
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        // todo: 여기서 계속 인증오류남
        val accessor = StompHeaderAccessor.wrap(message)
        val command = accessor.command ?: return message

        // 먼저 fallback 요청인지 확인 (URL에 '/xhr_send' 혹은 '/xhr_streaming'이 포함되어 있다면)
        val path = accessor.sessionAttributes?.get("sockJsPath") as? String ?: ""
        if (path.contains("/xhr_send") || path.contains("/xhr_streaming") || path.endsWith("/info")) {
            // fallback 전송 요청은 인증 없이 그대로 반환
            return message
        }

        // 세션 어트리뷰트에서 HandshakeInterceptor에서 넣어둔 인증 객체를 가져옴
        val sessionAttributes = accessor.sessionAttributes
        val authentication = sessionAttributes?.get("authentication") as? Authentication

        // 만약 인증 정보가 없다면, fallback 요청이 아닌 경우라면 로그를 남기하고 반환
        if (authentication == null) {
            logger.error { "인증 정보가 없습니다" }
            return message  // fallback이 아닌 일반 요청이라면 여기서 메시지를 차단할 수도 있지만,
            // fallback 요청에서는 이미 인증이 생략되어야 하므로 그냥 반환합니다.
        }

        // 이미 인증이 있는 경우, 커스텀 Principal을 설정
        val userId = authentication.name
        val stompPrincipal = StompPrincipal(userId)
        accessor.user = stompPrincipal

        // 만약 명령어가 SEND라면, 메시지 파싱 시도
        if (command == StompCommand.SEND) {
            // 만약 payload가 비어 있으면, 추가 검증 없이 그대로 반환
            val parsedMessage = getMessage(message) ?: return message
            // 여기에 필요하다면 추가 유효성 검사를 수행할 수 있습니다.
            validateMessage(parsedMessage)
        }
        return message
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

    /**
     * 이 메서드는 STOMP 메시지의 페이로드를 ChatMessageRequest 객체로 변환하는 역할을 합니다.
     */
    private fun getMessage(message: Message<*>): ChatMessageRequest? {
        val payload = message.payload
        // payload가 null이거나 빈 문자열이면 파싱하지 않고 null 반환
        if (payload == null || (payload is String && payload.trim().isEmpty())) {
            return null
        }

        return try {
            val payload = message.payload
            when (payload) {
                // JSON 문자열을 ChatMessageRequest로 변환
                is String -> objectMapper.readValue(payload, ChatMessageRequest::class.java)

                // 바이트 배열을 ChatMessageRequest로 변환
                is ByteArray -> objectMapper.readValue(payload, ChatMessageRequest::class.java)

                // 이미 ChatMessageRequest면 그대로 사용
                is ChatMessageRequest -> payload

                // 다른 형식이면 에러
                else -> throw WebSocketException("지원하지 않는 메시지 형식")
            }
        } catch (e: Exception) {
            throw WebSocketException("Failed to parse message")
        }
    }

}
