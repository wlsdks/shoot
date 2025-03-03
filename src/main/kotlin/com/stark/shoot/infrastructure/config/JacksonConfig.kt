package com.stark.shoot.infrastructure.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            // Kotlin 모듈 등록
            registerModule(kotlinModule())

            // JavaTimeModule 등록 (LocalDateTime 등 역직렬화)
            registerModule(JavaTimeModule())

            // 날짜·시간을 timestamp가 아닌 ISO-8601 문자 형태로
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

            // ObjectMapper의 설정을 변경하여 알 수 없는 필드를 무시하도록 할 수 있습니다.
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

            // null 필드를 직렬화 시 제외
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

}