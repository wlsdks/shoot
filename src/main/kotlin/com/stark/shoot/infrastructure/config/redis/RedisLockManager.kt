package com.stark.shoot.infrastructure.config.redis

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ErrorCode
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

    companion object {
        private const val LOCK_KEY_PREFIX = "lock:"
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
     *
     * @param lockKey 락 키
     * @param ownerId 락 소유자 ID (충돌 방지용)
     * @param action 락 획득 후 실행할 suspend 작업
     * @return 작업 결과
     */
    suspend fun <T> withLockSuspend(
        lockKey: String,
        ownerId: String,
        action: suspend () -> T  // 여기를 suspend 함수로 변경
    ): T {
        // 락 획득 시도
        val startTime = System.currentTimeMillis()
        var acquired = false
        var retryCount = 0
        var waitTime = 50L // 초기 대기 시간 50ms

        // 락 획득 시도 (타임아웃까지 반복)
        while (!acquired && System.currentTimeMillis() - startTime < lockWaitTimeout) {
            acquired = acquireLock(lockKey, ownerId)
            if (!acquired) {
                // 지수 백오프 적용 (최대 400ms까지)
                waitTime = minOf(waitTime * 2, 400)
                delay(waitTime)  // Thread.sleep 대신 코루틴 친화적인 delay 사용
                retryCount++
            }
        }

        // 락 획득 실패 시 예외 처리
        if (!acquired) {
            logger.warn { "Failed to acquire lock after $retryCount retries: $lockKey" }
            throw ApiException(
                "Failed to acquire lock: $lockKey after $lockWaitTimeout ms",
                ErrorCode.LOCK_ACQUIRE_FAILED
            )
        }

        // 락 획득 후 작업 실행
        try {
            logger.debug { "Executing action with lock: $lockKey" }
            return action()  // 여기서 suspend 함수 호출
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
        val redisKey = "$LOCK_KEY_PREFIX$lockKey"

        // SETNX 명령어로 락을 획득하고 자동 만료 시간 설정 (키가 없을 때만 설정)
        // - 이 방식은 락을 획득하고 명시적으로 해제하지 않더라도 timeout 후 자동 해제됨
        // - 서버 크래시 등으로 인한 락 고착 상태를 방지
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(redisKey, ownerId, Duration.ofMillis(timeout))

        // 락 획득 결과 로깅 및 반환
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
        val redisKey = "$LOCK_KEY_PREFIX$lockKey"

        try {
            // Lua 스크립트로 락 소유자 검증 후 삭제 (atomic 연산)
            // - 오직 락을 획득한 소유자만이 해제할 수 있도록 보장
            // - 다른 프로세스/스레드의 락을 실수로 해제하는 것을 방지
            val script = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
            """.trimIndent()

            // Lua 스크립트 실행 (락 소유자 검증 후 삭제)
            val result = redisTemplate.execute(
                RedisScript.of(script, Long::class.java),
                listOf(redisKey),
                ownerId
            ) ?: 0

            // 락 해제 결과 로깅
            val released = result == 1L
            if (released) {
                logger.debug { "Lock released: $lockKey by $ownerId" }
            } else {
                logger.warn { "Failed to release lock: $lockKey, current owner is not $ownerId" }
            }

            // 락 해제 결과 반환
            return released
        } catch (e: Exception) {
            logger.error(e) { "Error while releasing lock: $lockKey" }
            // 에러가 발생해도 false 반환 (락 해제 실패로 간주)
            return false
        }
    }

}