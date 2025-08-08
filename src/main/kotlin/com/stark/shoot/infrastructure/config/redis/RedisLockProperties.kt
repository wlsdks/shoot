package com.stark.shoot.infrastructure.config.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "redis.lock")
data class RedisLockProperties(
    var lockTimeout: Long = 10000,
    var lockWaitTimeout: Long = 2000,
    var maxRetries: Int = 3,
)
