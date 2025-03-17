package com.stark.shoot.infrastructure.config.async

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class ApplicationCoroutineScope {

    private val logger = KotlinLogging.logger {}
    // SupervisorJob() + Dispatchers.IO를 사용하여 I/O 작업에 최적화된 디스패처를 적용하고, 한 코루틴의 실패가 다른 코루틴에 영향을 주지 않도록 설계
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // 활성 작업 수를 추적하여 디버깅과 모니터링을 위한 용도로 사용
    private val activeJobs = AtomicInteger(0)

    /**
     * CoroutineScope를 사용해 비동기 작업을 실행합니다.
     *
     * @param block 비동기 작업
     * @return Job
     */
    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        activeJobs.incrementAndGet()
        return scope.launch {
            try {
                block()
            } catch (e: Exception) {
                // 중앙 집중식 예외 처리
                logger.error(e) { "코루틴 실행중 예외 발생" }
            } finally {
                activeJobs.decrementAndGet()
            }
        }
    }


    /**
     * 현재 활성화된 코루틴 개수를 반환합니다.
     *
     * @return 활성화된 코루틴 개수
     */
    fun getActiveJobCount(): Int = activeJobs.get()


    /**
     * 애플리케이션 종료 시 코루틴을 취소합니다.
     */
    @PreDestroy
    fun destroy() {
        logger.info { "애플리케이션 종료: ${activeJobs.get()}개 코루틴 취소 중" }
        scope.cancel()
    }

}
