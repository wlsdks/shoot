package com.stark.shoot.config

import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.adapter.out.persistence.mongodb.repository.MessageBookmarkMongoRepository
import com.stark.shoot.adapter.out.persistence.mongodb.repository.NotificationMongoRepository
import org.mockito.Mockito
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * 테스트용 MongoDB 설정
 * MongoDB를 사용하지 않는 테스트에서 mongoTemplate 및 MongoDB 리포지토리 빈 의존성 문제를 해결하기 위한 Mock 빈 제공
 */
@TestConfiguration
class TestMongoConfiguration {

    @Bean
    @Primary
    fun mongoTemplate(): MongoTemplate {
        return Mockito.mock(MongoTemplate::class.java)
    }

    @Bean
    @Primary
    fun chatMessageMongoRepository(): ChatMessageMongoRepository {
        return Mockito.mock(ChatMessageMongoRepository::class.java)
    }

    @Bean
    @Primary
    fun notificationMongoRepository(): NotificationMongoRepository {
        return Mockito.mock(NotificationMongoRepository::class.java)
    }

    @Bean
    @Primary
    fun messageBookmarkMongoRepository(): MessageBookmarkMongoRepository {
        return Mockito.mock(MessageBookmarkMongoRepository::class.java)
    }

    @Bean
    @ConditionalOnMissingBean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
