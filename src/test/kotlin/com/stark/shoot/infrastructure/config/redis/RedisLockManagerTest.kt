package com.stark.shoot.infrastructure.config.redis

import com.stark.shoot.domain.exception.web.LockAcquisitionException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.script.RedisScript
import java.time.Duration

@DisplayName("RedisLockManager 테스트")
class RedisLockManagerTest {

    private val redisTemplate: StringRedisTemplate = mock(StringRedisTemplate::class.java)
    private val valueOps: ValueOperations<String, String> = mock(ValueOperations::class.java) as ValueOperations<String, String>
    private lateinit var manager: RedisLockManager

    @BeforeEach
    fun setUp() {
        reset(redisTemplate, valueOps)
        val properties = RedisLockProperties()
        manager = RedisLockManager(redisTemplate, properties)
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
    }

    @Test
    @DisplayName("[happy] withLock 성공시 작업을 실행하고 락을 해제한다")
    fun withLockSuccess() {
        `when`(valueOps.setIfAbsent(eq("lock:res"), eq("owner"), any(Duration::class.java))).thenReturn(true)
        `when`(redisTemplate.execute(any<RedisScript<Long>>(), anyList(), eq("owner"))).thenReturn(1L)

        val result = manager.withLock("res", "owner") { "done" }

        assertThat(result).isEqualTo("done")
        verify(valueOps).setIfAbsent(eq("lock:res"), eq("owner"), any(Duration::class.java))
        verify(redisTemplate).execute(any<RedisScript<Long>>(), eq(listOf("lock:res")), eq("owner"))
    }

    @Test
    @DisplayName("[bad] withLock 미획득시 예외를 던진다")
    fun withLockFailure() {
        `when`(valueOps.setIfAbsent(eq("lock:res"), eq("owner"), any(Duration::class.java))).thenReturn(false)

        assertThrows(LockAcquisitionException::class.java) {
            manager.withLock("res", "owner", retryCount = 1) { "none" }
        }
        verify(valueOps, times(1)).setIfAbsent(eq("lock:res"), eq("owner"), any(Duration::class.java))
        verify(redisTemplate, never()).execute(any<RedisScript<Long>>(), anyList(), any())
    }

    @Test
    @DisplayName("extendLock 성공시 true 반환")
    fun extendLockSuccess() {
        `when`(redisTemplate.execute(any<RedisScript<Boolean>>(), anyList(), eq("owner"), any())).thenReturn(true)

        val result = manager.extendLock("lock:res", "owner", 1000L)

        assertThat(result).isTrue()
        verify(redisTemplate).execute(any<RedisScript<Boolean>>(), eq(listOf("lock:res")), eq("owner"), eq("1000"))
    }

    @Test
    @DisplayName("extendLock 실행중 예외 발생시 false 반환")
    fun extendLockException() {
        `when`(redisTemplate.execute(any<RedisScript<Boolean>>(), anyList(), any(), any())).thenThrow(RuntimeException("fail"))

        val result = manager.extendLock("lock:res", "owner", 1000L)

        assertThat(result).isFalse()
    }

    private fun invokeRelease(key: String, owner: String): Boolean {
        val method = RedisLockManager::class.java.getDeclaredMethod("releaseLock", String::class.java, String::class.java)
        method.isAccessible = true
        return method.invoke(manager, key, owner) as Boolean
    }

    @Test
    @DisplayName("releaseLock 성공시 true 반환")
    fun releaseLockSuccess() {
        `when`(redisTemplate.execute(any<RedisScript<Long>>(), anyList(), eq("owner"))).thenReturn(1L)

        val result = invokeRelease("lock:res", "owner")

        assertThat(result).isTrue()
        verify(redisTemplate).execute(any<RedisScript<Long>>(), eq(listOf("lock:res")), eq("owner"))
    }

    @Test
    @DisplayName("releaseLock 실패시 false 반환")
    fun releaseLockFailure() {
        `when`(redisTemplate.execute(any<RedisScript<Long>>(), anyList(), any())).thenReturn(0L)

        val result = invokeRelease("lock:res", "owner")

        assertThat(result).isFalse()
    }
}
