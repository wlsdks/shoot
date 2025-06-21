package com.stark.shoot.infrastructure.config.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

@DisplayName("RedisUtilService 테스트")
class RedisUtilServiceTest {

    private val redisTemplate = mock(StringRedisTemplate::class.java)
    private val valueOps = mock(ValueOperations::class.java) as ValueOperations<String, String>
    private val service = RedisUtilService(redisTemplate)

    @Test
    fun `값을 저장하고 조회할 수 있다`() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        `when`(valueOps.get("k")).thenReturn("v")

        val success = service.setValueSafely("k", "v", Duration.ofSeconds(1))
        assertThat(success).isTrue()
        val result = service.getValueSafely("k")
        assertThat(result).isEqualTo("v")
        verify(valueOps).set("k", "v", Duration.ofSeconds(1))
    }

    @Test
    fun `Redis 오류시 로컬 캐시 값을 사용한다`() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        doThrow(RuntimeException("fail")).`when`(valueOps).set("e", "v")
        doThrow(RuntimeException("fail")).`when`(valueOps).get("e")

        val success = service.setValueSafely("e", "v")
        assertThat(success).isFalse()
        val result = service.getValueSafely("e", defaultValue = "d")
        assertThat(result).isEqualTo("v")
    }
}
