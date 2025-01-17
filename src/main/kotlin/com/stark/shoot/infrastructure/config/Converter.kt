package com.stark.shoot.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.messaging.converter.MappingJackson2MessageConverter

@Bean
fun messageConverter(): MappingJackson2MessageConverter {
    val converter = MappingJackson2MessageConverter()
    converter.objectMapper = ObjectMapper().apply {
        findAndRegisterModules() // Kotlin 모듈 등록
    }
    return converter
}