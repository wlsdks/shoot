package com.stark.shoot.adapter.`in`.redis.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageMetadataRequest
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.domain.chat.message.type.MessageType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

@DisplayName("RedisMessageProcessor 테스트")
class RedisMessageProcessorTest {

    private val objectMapper = mock(ObjectMapper::class.java)
    private val webSocketMessageBroker = mock(WebSocketMessageBroker::class.java)
    private val processor = RedisMessageProcessor(objectMapper, webSocketMessageBroker)

    @Test
    @DisplayName("[happy] 스트림 키에서 채팅방 ID를 추출할 수 있다")
    fun `스트림 키에서 채팅방 ID를 추출할 수 있다`() {
        val roomId = processor.extractRoomIdFromStreamKey("stream:chat:room:123")
        assertThat(roomId).isEqualTo("123")
    }

    @Test
    @DisplayName("[happy] 잘못된 스트림 키는 null을 반환한다")
    fun `잘못된 스트림 키는 null을 반환한다`() {
        val roomId = processor.extractRoomIdFromStreamKey("invalid")
        assertThat(roomId).isNull()
    }

    @Test
    @DisplayName("[happy] 채널에서 채팅방 ID를 추출할 수 있다")
    fun `채널에서 채팅방 ID를 추출할 수 있다`() {
        val roomId = processor.extractRoomIdFromChannel("chat:room:456")
        assertThat(roomId).isEqualTo("456")
    }

    @Test
    @DisplayName("[happy] 잘못된 채널은 null을 반환한다")
    fun `잘못된 채널은 null을 반환한다`() {
        val roomId = processor.extractRoomIdFromChannel("wrong")
        assertThat(roomId).isNull()
    }

    @Test
    @DisplayName("[happy] 메시지를 성공적으로 처리할 수 있다")
    fun `메시지를 성공적으로 처리할 수 있다`() {
        val messageJson = "{}"
        val request = ChatMessageRequest(
            roomId = 1L,
            senderId = 2L,
            content = MessageContentRequest("t", MessageType.TEXT),
            metadata = ChatMessageMetadataRequest()
        )
        `when`(objectMapper.readValue(messageJson, ChatMessageRequest::class.java)).thenReturn(request)

        val result = processor.processMessage("1", messageJson)

        assertThat(result).isTrue()
        verify(webSocketMessageBroker).sendMessage("/topic/messages/1", request)
    }

    @Test
    @DisplayName("[bad] 메시지 처리 중 오류가 발생하면 false를 반환한다")
    fun `메시지 처리 중 오류가 발생하면 false를 반환한다`() {
        val messageJson = "{}"
        `when`(objectMapper.readValue(messageJson, ChatMessageRequest::class.java)).thenThrow(RuntimeException("fail"))

        val result = processor.processMessage("1", messageJson)

        assertThat(result).isFalse()
        verifyNoInteractions(webSocketMessageBroker)
    }
}
