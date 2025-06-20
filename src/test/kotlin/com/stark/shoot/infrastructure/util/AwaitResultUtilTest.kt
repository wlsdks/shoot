package com.stark.shoot.infrastructure.util

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture

@DisplayName("awaitResult 및 runAsync 유틸 테스트")
class AwaitResultUtilTest {

    @Test
    fun `CompletableFuture를 awaitResult 로 변환하여 값을 얻을 수 있다`() = runBlocking {
        val future = CompletableFuture.completedFuture(42)
        val result = future.awaitResult()
        assertThat(result).isEqualTo(42)
    }

    @Test
    fun `runAsync 는 결과를 가진 CompletableFuture 를 반환한다`() {
        val future = runAsync { 21 * 2 }
        assertThat(future.join()).isEqualTo(42)
    }

    @Test
    fun `runAsync 에서 예외가 발생하면 future 도 예외를 전달한다`() {
        val future = runAsync<Int> { throw IllegalStateException("fail") }
        assertThrows<IllegalStateException> { future.join() }
    }
}
