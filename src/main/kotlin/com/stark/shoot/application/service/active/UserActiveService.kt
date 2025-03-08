package com.stark.shoot.application.service.active

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.active.ChatActivity
import com.stark.shoot.application.port.`in`.active.UserActiveUseCase
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.TimeUnit

@UseCase
class UserActiveService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : UserActiveUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 사용자 활동 상태 업데이트
     * /app/active로 받은 메시지를 JSON으로 파싱해서 Redis에 active:<userId>:<roomId>로 저장. active 값은 "true" 또는 "false".
     * @param message 메시지
     */
    override fun updateUserActive(message: String) {
        try {
            val activity = objectMapper.readValue(message, ChatActivity::class.java)
            val key = "active:${activity.userId}:${activity.roomId}"
            // Redis에 사용자 활동 상태 저장
            // 사용자가 채팅방에 머물면 30초마다 "true" 갱신 → TTL(1분)이 만료 안 돼 창이 꺼지면 Heartbeat 멈추고 45초 후 "false"로 변경
            redisTemplate.opsForValue().set(key, activity.active.toString(), 45, TimeUnit.SECONDS)
            logger.info { "User activity updated: $key -> ${activity.active}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to process chat activity: $message" }
        }
    }

}