package com.stark.shoot.infrastructure.config.socket.interceptor

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageDeliveryException
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component

@Component
class RateLimitInterceptor(
    private val redisTemplate: StringRedisTemplate
) : ChannelInterceptor {

    override fun preSend(
        message: Message<*>,
        channel: MessageChannel
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        val userId = accessor.user?.name ?: return message

        val key = "ratelimit:chat:$userId"
        val count = redisTemplate.opsForValue().increment(key, 1)
        // java.time.Duration 사용
        redisTemplate.expire(key, java.time.Duration.ofMinutes(1))

        if (count!! > 30) { // null safety 처리
            throw MessageDeliveryException("Rate limit exceeded")
        }
        return message
    }

}