package com.stark.shoot.infrastructure.config.redis

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
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

    /**
     * 락 획득 후 작업 실행 (재시도 로직 포함)
     * @param lockKey 락 키
     * @param ownerId 락 소유자 ID
     * @param action 실행할 작업
     * @return 작업 실행 결과
     */
    fun <T> withLock(
        lockKey: String,
        ownerId: String,
        action: () -> T
    ): T {
        // 락 획득 시도
        val startTime = System.currentTimeMillis()
        var acquired = false

        while (!acquired && System.currentTimeMillis() - startTime < lockWaitTimeout) {
            acquired = acquireLock(lockKey, ownerId)
            if (!acquired) {
                Thread.sleep(100) // 잠시 대기 후 재시도
            }
        }

        if (!acquired) {
            throw ApiException(
                "Failed to acquire lock: $lockKey after $lockWaitTimeout ms",
                ErrorCode.LOCK_ACQUIRE_FAILED
            )
        }

        try {
            return action()
        } finally {
            releaseLock(lockKey, ownerId)
        }
    }

    /**
     * 분산 락 획득
     * @param lockKey 락 키
     * @param ownerId 락 소유자 ID
     * @param timeout 락 타임아웃 (밀리초)
     * @return 락 획득 여부
     */
    fun acquireLock(
        lockKey: String,
        ownerId: String,
        timeout: Long = lockTimeout
    ): Boolean {
        val redisKey = "lock:$lockKey"
        val expireTime = System.currentTimeMillis() + timeout

        // SETNX 명령어로 락을 획득하고 만료 시간 설정 (키가 없을 때만 설정)
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(redisKey, ownerId, Duration.ofMillis(timeout))

        // 락이 있으면 반환
        if (acquired == true) {
            logger.debug { "Lock acquired: $lockKey by $ownerId" }
            return true
        }

        logger.debug { "Failed to acquire lock: $lockKey" }
        return false
    }

    /**
     * 락 해제
     * @param lockKey 락 키
     * @param ownerId 락 소유자 ID
     * @return 락 해제 여부
     */
    fun releaseLock(
        lockKey: String,
        ownerId: String
    ): Boolean {
        val redisKey = "lock:$lockKey"

        // Lua 스크립트로 락 소유자 검증 후 삭제 (atomic 연산)
        val script = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
        """.trimIndent()

        val result = redisTemplate.execute(
            RedisScript.of(script, Long::class.java),
            listOf(redisKey),
            ownerId
        ) ?: 0

        val released = result == 1L
        if (released) {
            logger.debug { "Lock released: $lockKey by $ownerId" }
        } else {
            logger.warn { "Failed to release lock: $lockKey, current owner is not $ownerId" }
        }

        return released
    }

}