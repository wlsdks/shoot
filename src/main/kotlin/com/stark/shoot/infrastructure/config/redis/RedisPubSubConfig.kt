package com.stark.shoot.infrastructure.config.redis

import com.stark.shoot.adapter.`in`.redis.RedisMessageListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter

@Deprecated("Redis Stream 사용으로 대체")
@Configuration
class RedisPubSubConfig(
    private val redisMessageListener: RedisMessageListener
) {

    // Redis Pub/Sub 메시지 수신 컨테이너 빈 생성
    @Bean
    fun redisMessageListenerContainer(
        redisConnectionFactory: RedisConnectionFactory
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(redisConnectionFactory)

        // MessageListenerAdapter를 생성하고 초기화
        val adapter = MessageListenerAdapter(redisMessageListener, "onMessage")
        adapter.afterPropertiesSet()  // 반드시 호출하여 초기화

        // chat:room:* 패턴으로 모든 채팅방 채널 구독
        container.addMessageListener(
            adapter,
            PatternTopic("chat:room:*")
        )

        return container
    }

}