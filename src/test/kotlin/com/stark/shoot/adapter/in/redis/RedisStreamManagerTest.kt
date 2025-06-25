package com.stark.shoot.adapter.`in`.redis

import com.stark.shoot.adapter.`in`.redis.util.RedisStreamManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate

@DisplayName("RedisStreamManager 테스트")
class RedisStreamManagerTest {

    private val redisTemplate = mock(StringRedisTemplate::class.java)
    private val streamOps = mock(StreamOperations::class.java) as StreamOperations<String, String, String>
    private val manager = RedisStreamManager(redisTemplate)

    @Test
    @DisplayName("[happy] 메시지 ACK에 성공하면 true를 반환한다")
    fun `메시지 ACK에 성공하면 true를 반환한다`() {
        val recordId = RecordId.of("0-0")
        `when`(redisTemplate.opsForStream<String, String>()).thenReturn(streamOps)
        `when`(streamOps.acknowledge("cg", "key", recordId)).thenReturn(1L)

        val result = manager.acknowledgeMessage("key", "cg", recordId)

        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("[bad] 메시지 ACK 중 오류가 발생하면 false를 반환한다")
    fun `메시지 ACK 중 오류가 발생하면 false를 반환한다`() {
        val recordId = RecordId.of("0-0")
        `when`(redisTemplate.opsForStream<String, String>()).thenReturn(streamOps)
        `when`(streamOps.acknowledge("cg", "key", recordId)).thenThrow(RuntimeException("fail"))

        val result = manager.acknowledgeMessage("key", "cg", recordId)

        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("[happy] consumerId가 생성된다")
    fun `consumerId가 생성된다`() {
        val id = manager.getConsumerId()
        assertThat(id).isNotBlank()
    }
}
