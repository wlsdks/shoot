package com.stark.shoot.infrastructure.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture

/**
 * CompletableFuture를 코루틴으로 변환하는 확장 함수
 */
suspend fun <T> CompletableFuture<T>.awaitResult(): T = await()


/**
 * 코루틴 컨텍스트에서 CompletableFuture로 변환하는 함수
 * ApplicationCoroutineScope를 사용하여 중앙 집중식 코루틴 관리
 */
fun <T> runAsync(
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    block: suspend CoroutineScope.() -> T
): CompletableFuture<T> {
    val future = CompletableFuture<T>()

    coroutineScope.launch {
        try {
            val result = block()
            future.complete(result)
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
    }

    return future
}