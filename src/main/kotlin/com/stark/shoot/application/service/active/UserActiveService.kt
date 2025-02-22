package com.stark.shoot.application.service.active

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.active.ChatActivity
import com.stark.shoot.application.port.`in`.active.UserActiveUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class UserActiveService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : UserActiveUseCase {

    private val logger = KotlinLogging.logger {}
    
    override fun updateUserActive(message: String) {
        try {
            val activity = objectMapper.readValue(message, ChatActivity::class.java)
            val key = "active:${activity.userId}:${activity.roomId}"
            // Redis에 사용자 활동 상태 저장
            redisTemplate.opsForValue().set(key, activity.active.toString())
        } catch (e: Exception) {
            logger.error(e) { "Failed to process chat activity: $message" }
        }
    }

}