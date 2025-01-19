package com.stark.shoot.infrastructure.config.socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.exception.UnauthorizedException
import com.stark.shoot.infrastructure.common.exception.WebSocketException
import org.bson.types.ObjectId
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
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
    private val objectMapper: ObjectMapper
) : ChannelInterceptor {

    companion object {
        private const val TOPIC_PREFIX = "/topic/messages/"
        private const val QUEUE_PREFIX = "/queue/messages/"
    }

    /**
     * STOMP 프레임이 인바운드 채널을 통과하기 전에 호출됩니다.
     * 메시지의 STOMP 명령어와 헤더를 확인하여 특정 경로의 접근 권한을 검사할 수 있습니다.
     *
     * @param message 인바운드 메시지 (STOMP 프레임)
     * @param channel 메시지가 통과하는 채널
     * @return 원본 메시지를 그대로 반환하거나, 접근이 거부되면 예외 발생으로 처리를 중단
     * @throws AccessDeniedException 권한이 없는 사용자가 접근하려 할 경우 발생
     */
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        // STOMP 헤더를 래핑해 편하게 분석하기 위해 사용
        val accessor = StompHeaderAccessor.wrap(message)
        val userId = accessor.user?.name ?: throw UnauthorizedException("인증되지 않은 사용자")

        when (accessor.command) {
            // 구독 요청시: 해당 유저가 그 채팅방에 접근 권한이 있는지 확인
            StompCommand.SUBSCRIBE -> {
                val destination = accessor.destination
                if (destination?.startsWith(TOPIC_PREFIX) == true ||
                    destination?.startsWith(QUEUE_PREFIX) == true
                ) {
                    val roomId = getRoomIdFromDestination(destination)
                    validateRoomAccess(userId, roomId)
                }
            }

            // 메시지 전송 요청시: 메시지 형식이 올바른지, 발신자가 해당 채팅방 접근 권한이 있는지 확인
            StompCommand.SEND -> {
                val chatMessage = getMessage(message)
                validateMessage(chatMessage)
                validateRoomAccess(userId, chatMessage.roomId)
            }

            else -> { /* 다른 커맨드 처리 */
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
    private fun validateRoomAccess(userId: String, roomId: String) {
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
