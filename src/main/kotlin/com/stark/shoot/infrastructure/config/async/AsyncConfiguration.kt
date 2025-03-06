package com.stark.shoot.infrastructure.config.async

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class AsyncConfiguration {

    /**
     * 로깅을 비동기로 처리하기 위한 Executor
     */
    @Bean(name = ["logExecutor"])
    fun logExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 500
        executor.setThreadNamePrefix("AsyncLog-")
        executor.initialize()
        return executor
    }

}