package com.stark.shoot.infrastructure.config.socket.interceptor

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.exception.WebSocketException
import com.stark.shoot.infrastructure.config.socket.StompPrincipal
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.types.ObjectId
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.core.Authentication
import java.nio.file.AccessDeniedException

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
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)

        // 명령어 확인
        val command = accessor.command
            ?: return message

        // Handshake에서 저장했던 sessionAttributes를 확인
        val sessionAttributes = accessor.sessionAttributes
        val authentication = sessionAttributes?.get("authentication") as? Authentication

        // 만약 인증 성공했다면 accessor.user에 Principal 설정
        if (authentication != null) {
            // authentication.name == 인증 성공한 사용자 아이디(혹은 username)
            val userId = authentication.name
            // 커스텀 Principal 객체 생성
            val stompPrincipal = StompPrincipal(userId)

            // STOMP 세션의 principal로 설정
            accessor.user = stompPrincipal
        } else {
            logger.error { "인증 정보가 없습니다" }
        }

        val user = accessor.user?.name?.let { retrieveUserPort.findByUsername(username = it) }
        val userId = user?.id

        when (accessor.command) {
            StompCommand.CONNECT -> {
                logger.info { "Connected: $userId" }
            }

            // 구독 요청시: 해당 유저가 그 채팅방에 접근 권한이 있는지 확인
            StompCommand.SUBSCRIBE -> {
                val destination = accessor.destination
                if (destination?.startsWith(TOPIC_PREFIX) == true ||
                    destination?.startsWith(QUEUE_PREFIX) == true
                ) {
                    val roomId = getRoomIdFromDestination(destination)
                    if (userId != null) {
                        validateRoomAccess(userId, roomId)
                    }
                }
            }

            // 메시지 전송 요청시: 메시지 형식이 올바른지, 발신자가 해당 채팅방 접근 권한이 있는지 확인
            StompCommand.SEND -> {
                val chatMessage = getMessage(message)
                validateMessage(chatMessage)
                if (userId != null) {
                    validateRoomAccess(userId, chatMessage.roomId)
                }
            }

            else -> { /* 다른 커맨드(UNSUBSCRIBE 등) 처리 필요 시 추가 */
            }
        }

        // 별다른 문제가 없다면 메시지를 그대로 반환 -> 다음 단계로 진행
        return message
    }

    /**
     * STOMP destination에서 roomId 추출
     * 예: /topic/messages/123 -> 123
     */
    private fun getRoomIdFromDestination(destination: String?): String {
        return destination?.split("/")?.lastOrNull()
            ?: throw WebSocketException("Invalid destination format")
    }

    /**
     * 채팅방 접근 권한 검사
     */
    private fun validateRoomAccess(userId: ObjectId, roomId: String) {
        val chatRoom = loadChatRoomPort.findById(ObjectId(roomId))
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다")

        if (!chatRoom.participants.contains(userId)) {
            throw AccessDeniedException("채팅방에 접근 권한이 없습니다")
        }
    }

    /**
     * ChatMessageRequest 유효성 검사
     */
    private fun validateMessage(message: ChatMessageRequest) {
        if (message.roomId.isBlank()) {
            throw WebSocketException("Room ID is required")
        }
        if (message.content.length > 1000) {  // 예시: 1000자 제한
            throw WebSocketException("Message content too long")
        }
    }

    /**
     * 이 메서드는 STOMP 메시지의 페이로드를 ChatMessageRequest 객체로 변환하는 역할을 합니다.
     */
    private fun getMessage(message: Message<*>): ChatMessageRequest {
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
