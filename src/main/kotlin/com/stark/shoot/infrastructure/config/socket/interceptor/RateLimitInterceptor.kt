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

    // RateLimitInterceptor의 제한을 완화(예: 1분에 100회)하거나, /app/typing과 /app/active를 속도 제한에서 제외:
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        val destination = accessor.destination ?: return message
        if (destination == "/app/typing" || destination == "/app/active") return message // 제외

        val userId = accessor.user?.name ?: return message
        val key = "ratelimit:chat:$userId"
        val count = redisTemplate.opsForValue().increment(key, 1)
        redisTemplate.expire(key, java.time.Duration.ofMinutes(1))

        if (count!! > 300) { // 제한 완화
            throw MessageDeliveryException("Rate limit exceeded")
        }

        return message
    }

}