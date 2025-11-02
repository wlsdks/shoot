package com.stark.shoot.infrastructure.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.OptimisticLockException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.support.RetryTemplate

/**
 * Spring Retry 설정
 *
 * **목적:**
 * - OptimisticLockException 발생 시 자동 재시도
 * - 동시 수정 충돌을 자동으로 처리하여 사용자 경험 개선
 *
 * **배경:**
 * - JPA @Version 필드로 낙관적 락 적용
 * - 동시 수정 시 OptimisticLockException 발생
 * - 재시도를 통해 대부분의 충돌을 투명하게 해결
 *
 * **재시도 전략:**
 * - 최대 3번 재시도 (총 4번 시도)
 * - 지수 백오프: 100ms → 200ms → 400ms
 * - 최대 대기 시간: 1초
 *
 * **적용 대상:**
 * - @Retryable(
 *     retryFor = [OptimisticLockException::class],
 *     maxAttempts = 3,
 *     backoff = Backoff(delay = 100, multiplier = 2.0, maxDelay = 1000)
 *   )
 *
 * **사용 예시:**
 * ```kotlin
 * @Service
 * class FriendRequestService {
 *     @Retryable(
 *         retryFor = [OptimisticLockException::class],
 *         maxAttempts = 3,
 *         backoff = Backoff(delay = 100, multiplier = 2.0)
 *     )
 *     @Transactional
 *     fun acceptFriendRequest(requestId: Long) {
 *         val request = friendRequestRepository.findById(requestId)
 *         request.accept() // version 충돌 시 재시도
 *         friendRequestRepository.save(request)
 *     }
 * }
 * ```
 */
@Configuration
@EnableRetry
class RetryConfig {

    private val logger = KotlinLogging.logger {}

    /**
     * OptimisticLockException 재시도 정책
     *
     * 동시성 충돌이 일시적인 경우가 많기 때문에
     * 짧은 지연 후 재시도하면 대부분 성공
     */
    @Bean
    fun optimisticLockRetryTemplate(): RetryTemplate {
        return RetryTemplate.builder()
            .maxAttempts(3) // 최대 3번 재시도
            .exponentialBackoff(
                100,    // 초기 대기: 100ms
                2.0,    // 지수 배율: 2배씩 증가
                1000    // 최대 대기: 1초
            )
            .retryOn(OptimisticLockException::class.java)
            .withListener(OptimisticLockRetryListener())
            .build()
    }

    /**
     * 재시도 이벤트 로깅 리스너
     *
     * 디버깅 및 모니터링을 위한 로그 출력
     */
    private class OptimisticLockRetryListener : org.springframework.retry.RetryListener {
        private val logger = KotlinLogging.logger {}

        override fun <T : Any?, E : Throwable?> open(
            context: org.springframework.retry.RetryContext?,
            callback: org.springframework.retry.RetryCallback<T, E>?
        ): Boolean {
            logger.debug { "낙관적 락 재시도 시작: ${context?.retryCount ?: 0}번째 시도" }
            return true
        }

        override fun <T : Any?, E : Throwable?> onSuccess(
            context: org.springframework.retry.RetryContext?,
            callback: org.springframework.retry.RetryCallback<T, E>?,
            result: T & Any
        ) {
            if ((context?.retryCount ?: 0) > 0) {
                logger.info {
                    "낙관적 락 충돌 해결 성공: ${context?.retryCount}번의 재시도 후 성공"
                }
            }
        }

        override fun <T : Any?, E : Throwable?> onError(
            context: org.springframework.retry.RetryContext?,
            callback: org.springframework.retry.RetryCallback<T, E>?,
            throwable: Throwable?
        ) {
            logger.warn {
                "낙관적 락 재시도 실패 (${context?.retryCount}/${context?.getAttribute("maxAttempts")}): " +
                "${throwable?.message}"
            }
        }
    }
}
