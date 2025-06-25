package com.stark.shoot.adapter.out.redis.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageMetadataRequest
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.domain.chat.message.type.MessageType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.connection.stream.StreamOperations
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate

@DisplayName("PublishMessageRedisAdapter 테스트")
class PublishMessageRedisAdapterTest {

    private val redisTemplate = mock(StringRedisTemplate::class.java)
    private val streamOps = mock(StreamOperations::class.java) as StreamOperations<String, String, *>
    private val objectMapper = mock(ObjectMapper::class.java)
    private val adapter = PublishMessageRedisAdapter(redisTemplate, objectMapper)

    @Test
    @DisplayName("[happy] 메시지를 Redis Stream에 발행할 수 있다")
    fun `메시지를 Redis Stream에 발행할 수 있다`() {
        val request = ChatMessageRequest(
            roomId = 1L,
            senderId = 2L,
            content = MessageContentRequest("text", MessageType.TEXT),
            metadata = ChatMessageMetadataRequest()
        )
        val json = "{}"
        `when`(objectMapper.writeValueAsString(request)).thenReturn(json)
        `when`(redisTemplate.opsForStream<String, String>()).thenReturn(streamOps)
        `when`(streamOps.add(any())).thenReturn(RecordId.of("0-0"))

        adapter.publish(request)

        verify(streamOps).add(any())
        verify(objectMapper).writeValueAsString(request)
    }
}
