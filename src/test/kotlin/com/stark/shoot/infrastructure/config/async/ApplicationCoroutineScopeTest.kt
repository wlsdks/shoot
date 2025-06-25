package com.stark.shoot.infrastructure.config.async

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ApplicationCoroutineScope 테스트")
class ApplicationCoroutineScopeTest {

    private val scope = ApplicationCoroutineScope()

    @Test
    @DisplayName("[happy] launch 실행 후 활성 코루틴 수가 감소한다")
    fun `launch 실행 후 활성 코루틴 수가 감소한다`() = runBlocking {
        val job = scope.launch { delay(50) }
        assertThat(scope.getActiveJobCount()).isEqualTo(1)
        job.join()
        delay(10)
        assertThat(scope.getActiveJobCount()).isEqualTo(0)
    }

    @Test
    @DisplayName("[bad] 예외 발생시에도 활성 코루틴 수가 감소한다")
    fun `예외 발생시에도 활성 코루틴 수가 감소한다`() = runBlocking {
        val job = scope.launch { throw RuntimeException("error") }
        job.join()
        delay(10)
        assertThat(scope.getActiveJobCount()).isEqualTo(0)
    }
}
