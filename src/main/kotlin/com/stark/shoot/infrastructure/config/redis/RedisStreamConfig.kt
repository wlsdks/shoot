package com.stark.shoot.infrastructure.config.redis

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Duration

@Configuration
class RedisStreamConfig {

    @Bean
    fun streamCleanupScheduler(
        redisTemplate: StringRedisTemplate
    ): TaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()

        // 스케줄러 설정
        return scheduler.apply {
            poolSize = 1
            setThreadNamePrefix("redis-stream-cleanup-") // set 메서드 직접 호출
            initialize()

            // 주기적으로 오래된 메시지 정리 (선택사항)
            scheduleAtFixedRate({
                try {
                    val streamKeys = redisTemplate.keys("stream:chat:room:*")
                    streamKeys.forEach { key ->
                        // 24시간 이상 된 메시지 정리 (XTRIM)
                        // 최대 1000개만 유지 (최신 메시지 우선)
                        redisTemplate.opsForStream<Any, Any>()
                            .trim(key, 1000, true)
                    }
                } catch (e: Exception) {
                    // 오류 처리
                }
            }, Duration.ofHours(1))
        }
    }

}