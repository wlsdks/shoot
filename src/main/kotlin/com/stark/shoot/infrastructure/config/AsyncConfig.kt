package com.stark.shoot.infrastructure.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * 비동기 처리 설정
 *
 * 이벤트 리스너에서 @Async를 사용하여 비동기 처리를 할 수 있도록 설정합니다.
 * 주요 사용처:
 * - WebSocket 브로드캐스트 (메시지 저장 후 비동기 전송)
 * - 알림 전송 (이메일, 푸시 등)
 * - 로깅 및 감사 (audit log)
 */
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {

    private val logger = KotlinLogging.logger {}

    /**
     * 비동기 작업용 ThreadPool Executor 설정
     *
     * 설정값:
     * - corePoolSize: 5 (기본 스레드 수)
     * - maxPoolSize: 20 (최대 스레드 수)
     * - queueCapacity: 100 (큐 용량)
     * - threadNamePrefix: "async-" (스레드 이름 prefix)
     * - keepAliveSeconds: 60 (유휴 스레드 유지 시간)
     */
    @Bean(name = ["taskExecutor"])
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 20
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("async-")
        executor.setKeepAliveSeconds(60)
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()

        logger.info { "Async executor initialized: corePoolSize=5, maxPoolSize=20, queueCapacity=100" }

        return executor
    }

    /**
     * 비동기 작업 중 발생한 예외 처리
     *
     * @Async 메서드에서 발생한 예외를 로깅합니다.
     * 비동기 작업이므로 예외가 발생해도 원본 메서드에는 영향이 없습니다.
     */
    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return AsyncUncaughtExceptionHandler { ex, method, params ->
            logger.error(ex) {
                "Async method execution failed: method=${method.name}, " +
                "params=${params.contentToString()}, error=${ex.message}"
            }
        }
    }
}
