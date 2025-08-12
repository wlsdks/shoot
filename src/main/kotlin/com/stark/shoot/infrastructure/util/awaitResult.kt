package com.stark.shoot.infrastructure.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 */
fun <T> runAsync(block: suspend CoroutineScope.() -> T): CompletableFuture<T> {
    val future = CompletableFuture<T>()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val result = block()
            future.complete(result)
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
    }

    return future
}