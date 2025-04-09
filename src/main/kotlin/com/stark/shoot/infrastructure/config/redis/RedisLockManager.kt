package com.stark.shoot.infrastructure.config.redis

import com.stark.shoot.infrastructure.exception.web.LockAcquisitionException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisLockManager(
    private val redisTemplate: StringRedisTemplate
) {
    private val logger = KotlinLogging.logger {}
    private val lockTimeout = 10000L    // 10초
    private val lockWaitTimeout = 2000L // 2초
    private val maxRetries = 3          // 최대 재시도 횟수

    companion object {
        private const val LOCK_KEY_PREFIX = "lock:"

        // Lua 스크립트를 상수로 선언하여 재사용
        private const val RELEASE_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
        """
    }

    /**
     * 일반 함수: Redis 분산 락을 획득하여 작업을 실행합니다.
     *
     * @param lockKey 락 키
     * @param ownerId 락 소유자 ID (충돌 방지용)
     * @param action 락 획득 후 실행할 작업
     * @return 작업 결과
     */
    fun <T> withLock(lockKey: String, ownerId: String, action: () -> T): T {
        val isAcquired = acquireLock(lockKey, ownerId)
        if (!isAcquired) {
            throw LockAcquisitionException("분산 락 획득 실패: $lockKey")
        }

        try {
            return action()
        } finally {
            releaseLock(lockKey, ownerId)
        }
    }

    /**
     * 코루틴 지원 함수: Redis 분산 락을 획득하여 suspend 작업을 실행합니다.
     */
    suspend fun <T> withLockSuspend(
        lockKey: String,
        ownerId: String,
        action: suspend () -> T
    ): T {
        val fullLockKey = "$LOCK_KEY_PREFIX$lockKey"

        // 락 획득 시도 (지수 백오프 적용)
        var acquired = false
        var retryCount = 0

        while (!acquired && retryCount < maxRetries) {
            acquired = acquireLock(fullLockKey, ownerId)
            if (acquired) break

            // 지수 백오프: 재시도 간격을 점점 늘림 (최대 1초까지)
            val waitTime = minOf(100L * (1 shl retryCount), 1000L)
            delay(waitTime)
            retryCount++

            logger.debug { "Lock acquisition retry #${retryCount} for key: $lockKey, waiting ${waitTime}ms" }
        }

        // 락 획득 실패 시 예외 발생
        if (!acquired) {
            logger.warn { "Failed to acquire lock after $retryCount retries: $lockKey" }
            throw LockAcquisitionException("Failed to acquire lock: $lockKey after $maxRetries retries")
        }

        // 락 획득 성공 시 작업 실행
        try {
            logger.debug { "Executing action with lock: $lockKey" }
            return action()
        } finally {
            releaseLock(fullLockKey, ownerId)
        }
    }

    /**
     * 분산 락 획득 - 내부 메서드
     */
    private fun acquireLock(
        fullLockKey: String,
        ownerId: String,
        timeout: Long = lockTimeout
    ): Boolean {
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(fullLockKey, ownerId, Duration.ofMillis(timeout))

        if (acquired == true) {
            logger.debug { "Lock acquired: $fullLockKey by $ownerId" }
            return true
        }

        return false
    }

    /**
     * 락 해제 - 내부 메서드
     */
    private fun releaseLock(
        fullLockKey: String,
        ownerId: String
    ): Boolean {
        try {
            // Lua 스크립트 실행 (락 소유자 검증 후 삭제)
            val result = redisTemplate.execute(
                RedisScript.of(RELEASE_SCRIPT, Long::class.java),
                listOf(fullLockKey),
                ownerId
            ) ?: 0

            val released = result == 1L
            if (released) {
                logger.debug { "Lock released: $fullLockKey by $ownerId" }
            }

            return released
        } catch (e: Exception) {
            logger.error(e) { "Error while releasing lock: $fullLockKey" }
            return false
        }
    }

}