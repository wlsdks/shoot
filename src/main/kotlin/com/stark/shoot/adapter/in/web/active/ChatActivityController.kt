package com.stark.shoot.adapter.`in`.web.active

import com.stark.shoot.adapter.`in`.web.dto.active.ChatActivity
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller

@Controller
class ChatActivityController(
    private val redisTemplate: StringRedisTemplate
) {

    /**
     * user1과 user2가 room123에 있을 때, user1이 채팅방을 나가면 Redis에 active:user1:room123이 "false"로 설정.
     * 만약 user1이 다시 채팅방에 들어오면 active:user1:room123을 "true"로 설정.
     * true: 채팅방에 참여 중, false: 채팅방에 참여하지 않음
     */
    @MessageMapping("/active")
    fun handleActivity(@Payload activity: ChatActivity) {
        redisTemplate.opsForValue()
            .set("active:${activity.userId}:${activity.roomId}", activity.active.toString())
    }

}