package com.stark.shoot.application.service.concurrency

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.Result

/**
 * 동시성 테스트를 위한 유틸리티 클래스
 *
 * 여러 작업을 동시에 실행하고 결과를 수집합니다.
 * Race Condition 테스트에 유용합니다.
 *
 * 사용 예:
 * ```kotlin
 * val executor = ConcurrentTestExecutor(threadCount = 2)
 * val results = executor.executeAll(
 *     { sendFriendRequest(userA, userB) },
 *     { sendFriendRequest(userB, userA) }
 * )
 * ```
 */
class ConcurrentTestExecutor(
    private val threadCount: Int = 2,
    private val timeoutSeconds: Long = 10
) {
    /**
     * 여러 작업을 동시에 실행하고 결과를 반환합니다.
     *
     * @param tasks 실행할 작업 목록
     * @return 각 작업의 Result 목록 (성공/실패)
     */
    fun <T> executeAll(vararg tasks: () -> T): List<Result<T>> {
        require(tasks.size <= threadCount) {
            "Task count (${tasks.size}) must not exceed thread count ($threadCount)"
        }

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(tasks.size)
        val results = mutableListOf<Result<T>>()

        try {
            // 모든 태스크를 동시에 시작
            tasks.forEach { task ->
                executor.submit {
                    try {
                        latch.countDown() // 모든 스레드가 준비될 때까지 대기
                        latch.await()      // 모든 스레드 동시 시작

                        val result = task()
                        synchronized(results) {
                            results.add(Result.success(result))
                        }
                    } catch (e: Exception) {
                        synchronized(results) {
                            results.add(Result.failure(e))
                        }
                    }
                }
            }

            // 모든 작업 완료 대기
            executor.shutdown()
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                throw IllegalStateException("Timeout waiting for tasks to complete")
            }

            return results.toList()
        } finally {
            executor.shutdownNow()
        }
    }

    /**
     * 동일한 작업을 N번 동시에 실행합니다.
     *
     * @param count 반복 횟수
     * @param task 실행할 작업
     * @return 각 작업의 Result 목록
     */
    fun <T> executeParallel(count: Int, task: () -> T): List<Result<T>> {
        return executeAll(*Array(count) { task })
    }
}

/**
 * Result 확장 함수: 성공한 결과만 필터링
 */
fun <T> List<Result<T>>.successes(): List<T> {
    return this.mapNotNull { it.getOrNull() }
}

/**
 * Result 확장 함수: 실패한 예외만 필터링
 */
fun <T> List<Result<T>>.failures(): List<Throwable> {
    return this.mapNotNull { it.exceptionOrNull() }
}

/**
 * Result 확장 함수: 특정 타입의 예외만 필터링
 */
inline fun <reified E : Throwable, T> List<Result<T>>.failuresOfType(): List<E> {
    return this.failures().filterIsInstance<E>()
}
