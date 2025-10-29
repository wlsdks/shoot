package com.stark.shoot.infrastructure.config.redis

import com.stark.shoot.domain.exception.web.LockAcquisitionException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisLockManager(
    private val redisTemplate: StringRedisTemplate,
    private val properties: RedisLockProperties,
) {
    private val logger = KotlinLogging.logger {}
    private val lockTimeout get() = properties.lockTimeout
    private val lockWaitTimeout get() = properties.lockWaitTimeout
    private val maxRetries get() = properties.maxRetries

    // 락 해제를 위한 Lua 스크립트를 한 번만 생성하여 재사용
    private val releaseScript = RedisScript.of(RELEASE_SCRIPT, Long::class.java)

    // 락 연장을 위한 Lua 스크립트
    private val extendScript = RedisScript.of(EXTEND_SCRIPT, Boolean::class.java)

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

        // 락 연장을 위한 Lua 스크립트
        private const val EXTEND_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('pexpire', KEYS[1], ARGV[2])
            else
                return 0
            end
        """
    }

    /**
     * 일반 함수: Redis 분산 락을 획득하여 작업을 실행합니다.
     * 락 획득에 실패할 경우 재시도 로직을 사용합니다.
     *
     * @param lockKey 락 키
     * @param ownerId 락 소유자 ID (충돌 방지용)
     * @param retryCount 최대 재시도 횟수 (기본값: maxRetries)
     * @param autoExtend 작업 실행 중 자동으로 락 타임아웃을 연장할지 여부
     * @param action 락 획득 후 실행할 작업
     * @return 작업 결과
     */
    fun <T> withLock(
        lockKey: String,
        ownerId: String,
        retryCount: Int = maxRetries,
        autoExtend: Boolean = false,
        action: () -> T
    ): T {
        val fullLockKey = "$LOCK_KEY_PREFIX$lockKey"

        // 락 획득 시도 (지수 백오프 적용)
        var acquired = false
        var currentRetry = 0

        while (!acquired && currentRetry < retryCount) {
            acquired = acquireLock(fullLockKey, ownerId)
            if (acquired) break

            // 지수 백오프: 재시도 간격을 점점 늘림 (최대 1초까지)
            val waitTime = minOf(100L * (1 shl currentRetry), 1000L)
            try {
                Thread.sleep(waitTime)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                logger.warn { "Lock acquisition interrupted for key: $lockKey" }
                throw LockAcquisitionException("Lock acquisition interrupted: $lockKey")
            }

            currentRetry++
            logger.debug { "Lock acquisition retry #${currentRetry} for key: $lockKey, waiting ${waitTime}ms" }
        }

        // 락 획득 실패 시 예외 발생
        if (!acquired) {
            logger.warn { "Failed to acquire lock after $currentRetry retries: $lockKey" }
            throw LockAcquisitionException("Failed to acquire lock: $lockKey after $retryCount retries")
        }

        // 자동 연장 스레드
        val extendThread = if (autoExtend) {
            startLockExtender(fullLockKey, ownerId)
        } else null

        try {
            logger.debug { "Executing action with lock: $lockKey" }
            return action()
        } finally {
            // 자동 연장 스레드 중지
            extendThread?.interrupt()

            // 락 해제
            releaseLock(fullLockKey, ownerId)
        }
    }

    /**
     * 락 타임아웃을 자동으로 연장하는 백그라운드 스레드를 시작합니다.
     *
     * @param lockKey 락 키
     * @param ownerId 락 소유자 ID
     * @return 생성된 스레드 (중지를 위해 반환)
     */
    private fun startLockExtender(lockKey: String, ownerId: String): Thread {
        val extendInterval = lockTimeout / 3  // 타임아웃의 1/3마다 연장

        val thread = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    Thread.sleep(extendInterval)
                    extendLock(lockKey, ownerId, lockTimeout)
                }
            } catch (e: InterruptedException) {
                // 스레드 종료 요청 - 정상적인 흐름
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                logger.error(e) { "Error in lock extender thread for key: $lockKey" }
            }
        }

        thread.isDaemon = true  // 데몬 스레드로 설정하여 JVM 종료 시 자동 종료
        thread.name = "lock-extender-$lockKey"
        thread.start()

        return thread
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
     * 락 타임아웃을 연장합니다.
     *
     * @param lockKey 락 키
     * @param ownerId 락 소유자 ID
     * @param timeoutMillis 연장할 타임아웃 (밀리초)
     * @return 연장 성공 여부
     */
    fun extendLock(
        lockKey: String,
        ownerId: String,
        timeoutMillis: Long = lockTimeout
    ): Boolean {
        try {
            // Lua 스크립트 실행 (락 소유자 검증 후 타임아웃 연장)
            val result = redisTemplate.execute(
                extendScript,
                listOf(lockKey),
                ownerId,
                timeoutMillis.toString()
            ) ?: false

            if (result) {
                logger.debug { "Lock extended: $lockKey by $ownerId for ${timeoutMillis}ms" }
            } else {
                logger.warn { "Failed to extend lock: $lockKey (lock not owned or expired)" }
            }

            return result
        } catch (e: Exception) {
            logger.error(e) { "Error while extending lock: $lockKey - ${e.message}" }
            return false
        }
    }

    /**
     * 락 해제 - 내부 메서드
     */
    private fun releaseLock(
        fullLockKey: String,
        ownerId: String
    ): Boolean {
        try {
            // 미리 생성된 Lua 스크립트 사용 (락 소유자 검증 후 삭제)
            val result = redisTemplate.execute(
                releaseScript,
                listOf(fullLockKey),
                ownerId
            ) ?: 0

            val released = result == 1L
            if (released) {
                logger.debug { "Lock released: $fullLockKey by $ownerId" }
            } else {
                logger.warn { "Failed to release lock: $fullLockKey (lock not owned or already expired)" }
            }

            return released
        } catch (e: Exception) {
            logger.error(e) { "Error while releasing lock: $fullLockKey - ${e.message}" }
            return false
        }
    }

}
