package com.stark.shoot.adapter.`in`.redis.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.rest.socket.WebSocketMessageBroker
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Redis 메시지 처리를 위한 공통 유틸리티 클래스
 *
 * 이 클래스는 Redis Stream과 Pub/Sub에서 공통으로 사용되는 메시지 처리 로직을 제공합니다.
 */
@Component
class RedisMessageProcessor(
    private val objectMapper: ObjectMapper,
    private val webSocketMessageBroker: WebSocketMessageBroker
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        // 채팅방 ID 추출을 위한 정규식 패턴
        private val STREAM_ROOM_ID_PATTERN = Regex("stream:chat:room:([^:]+)")
        private val PUBSUB_ROOM_ID_PATTERN = Regex("chat:room:([^:]+)")
    }

    /**
     * Redis Stream 메시지에서 채팅방 ID를 추출합니다.
     *
     * @param streamKey Redis Stream 키
     * @return 추출된 채팅방 ID 또는 null
     */
    fun extractRoomIdFromStreamKey(streamKey: String): String? {
        val roomId = STREAM_ROOM_ID_PATTERN.find(streamKey)
            ?.groupValues
            ?.getOrNull(1)

        if (roomId == null) {
            logger.warn { "Could not extract roomId from stream key: $streamKey" }
        }

        return roomId
    }

    /**
     * Redis Pub/Sub 채널에서 채팅방 ID를 추출합니다.
     *
     * @param channel Redis Pub/Sub 채널
     * @return 추출된 채팅방 ID 또는 null
     */
    fun extractRoomIdFromChannel(channel: String): String? {
        val roomId = PUBSUB_ROOM_ID_PATTERN.find(channel)?.groupValues?.getOrNull(1)
        if (roomId == null) {
            logger.warn { "Could not extract roomId from channel: $channel" }
        }
        return roomId
    }

    /**
     * JSON 문자열을 ChatMessageRequest 객체로 변환합니다.
     *
     * @param messageJson JSON 형식의 메시지 문자열
     * @return 변환된 ChatMessageRequest 객체
     */
    fun parseChatMessage(messageJson: String): ChatMessageRequest {
        return objectMapper.readValue(messageJson, ChatMessageRequest::class.java)
    }

    /**
     * 채팅 메시지를 WebSocket을 통해 클라이언트에게 전송합니다.
     *
     * @param roomId 채팅방 ID
     * @param chatMessage 전송할 채팅 메시지
     */
    fun sendMessageToWebSocket(roomId: String, chatMessage: ChatMessageRequest) {
        webSocketMessageBroker.sendMessage("/topic/messages/$roomId", chatMessage)
    }

    /**
     * 메시지 처리 과정을 통합적으로 수행합니다.
     *
     * @param roomId 채팅방 ID
     * @param messageJson JSON 형식의 메시지 문자열
     * @return 처리 성공 여부
     */
    fun processMessage(roomId: String, messageJson: String): Boolean {
        return try {
            val chatMessage = parseChatMessage(messageJson)
            sendMessageToWebSocket(roomId, chatMessage)
            true
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 오류: $messageJson" }
            false
        }
    }
}