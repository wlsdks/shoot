package com.stark.shoot.infrastructure.config.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.domain.chat.message.ChatMessage
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

    // StringRedisTemplate 빈 생성
    @Bean(name = ["stringRedisTemplate"])
    fun stringRedisTemplate(
        connectionFactory: RedisConnectionFactory
    ): StringRedisTemplate {
        return StringRedisTemplate().apply { setConnectionFactory(connectionFactory) }
    }

    // ChatMessage 객체를 JSON으로 직렬화하는 RedisTemplate 빈 생성
    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper
    ): RedisTemplate<String, ChatMessage> {
        val template = RedisTemplate<String, ChatMessage>()
        template.connectionFactory = connectionFactory

        // 키는 String, 값은 ChatMessage를 JSON으로 직렬화
        template.keySerializer = StringRedisSerializer()
        val serializer = Jackson2JsonRedisSerializer(objectMapper, ChatMessage::class.java)
        template.valueSerializer = serializer

        // Hash 데이터 타입에 대한 직렬화 설정
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = serializer

        // 직렬화 설정 적용
        template.afterPropertiesSet()
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