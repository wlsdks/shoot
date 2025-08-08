package com.stark.shoot.infrastructure.config.redis

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

// Jackson 기반 직렬화를 사용해 ChatMessage를 자동으로 JSON으로 변환하도록 설정
@Configuration
class RedisConfig {

    @Bean
    fun stringRedisTemplate(
        connectionFactory: RedisConnectionFactory
    ): StringRedisTemplate {
        val template = StringRedisTemplate()
        template.connectionFactory = connectionFactory
        return template
    }

    // RedisConfig 클래스에 추가
    @Bean
    fun redisPublisherTemplate(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper
    ): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = Jackson2JsonRedisSerializer(objectMapper, Any::class.java)
        template.afterPropertiesSet()
        return template
    }

}