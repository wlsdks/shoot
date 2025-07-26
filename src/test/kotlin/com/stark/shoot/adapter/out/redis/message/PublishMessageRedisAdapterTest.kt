package com.stark.shoot.adapter.out.redis.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageMetadataRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageContentRequest
import com.stark.shoot.domain.chat.message.type.MessageType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate

@DisplayName("PublishMessageRedisAdapter 테스트")
class PublishMessageRedisAdapterTest {

    private val redisTemplate = mock(StringRedisTemplate::class.java)
    private val streamOps = mock(StreamOperations::class.java) as StreamOperations<String, String, String>

    // Use a real ObjectMapper instead of a mock for better test fidelity
    private val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(KotlinModule.Builder().build())
    }

    private val adapter = PublishMessageRedisAdapter(redisTemplate, objectMapper)

    @Test
    @DisplayName("[happy] 메시지를 Redis Stream에 발행할 수 있다")
    fun `메시지를 Redis Stream에 발행할 수 있다`() {
        // given
        val request = ChatMessageRequest(
            roomId = 1L,
            senderId = 2L,
            content = MessageContentRequest("text", MessageType.TEXT),
            metadata = ChatMessageMetadataRequest()
        )
        `when`(redisTemplate.opsForStream<String, String>()).thenReturn(streamOps)
        `when`(streamOps.add(any())).thenReturn(RecordId.of("0-0"))

        // when
        runBlocking {
            adapter.publish(request)
        }

        // then
        verify(redisTemplate).opsForStream<String, String>()
        // Verify that add() was called with any record
        verify(streamOps).add(any())
    }

    @Test
    @DisplayName("[error] Redis 연결 실패시 예외가 발생한다")
    fun `Redis 연결 실패시 예외가 발생한다`() {
        // given
        val request = ChatMessageRequest(
            roomId = 1L,
            senderId = 2L,
            content = MessageContentRequest("text", MessageType.TEXT),
            metadata = ChatMessageMetadataRequest()
        )
        `when`(redisTemplate.opsForStream<String, String>()).thenReturn(streamOps)
        `when`(streamOps.add(any())).thenThrow(RedisConnectionFailureException("Connection failed"))

        // when & then
        assertThrows<RedisConnectionFailureException> {
            runBlocking {
                adapter.publish(request)
            }
        }
    }
}
