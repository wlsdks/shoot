package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.domain.chat.message.ChatMessage
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class UnreadCountUpdateFilter(
    private val redisTemplate: StringRedisTemplate
) : MessageProcessingFilter {

    override fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 읽음 상태 초기화 - 발신자는 항상 읽음 처리
        if (!message.readBy.containsKey(message.senderId)) {
            message.readBy[message.senderId] = true
        }

        // Redis에 활성 사용자 상태 조회는 한 번만 수행
        val roomId = message.roomId
        val activeUsersKeys = redisTemplate.keys("active:*:$roomId")
        val activeUserIds = activeUsersKeys.mapNotNull { key ->
            val isActive = redisTemplate.opsForValue().get(key) == "true"
            if (isActive) {
                key.substring(key.indexOf(':') + 1, key.lastIndexOf(':'))
                    .toLongOrNull()
            } else null
        }.toSet()

        // 이미 읽은 사용자 및 활성 사용자는 readBy에 추가
        activeUserIds.forEach { userId ->
            if (userId != message.senderId && !message.readBy.containsKey(userId)) {
                message.readBy[userId] = true
            }
        }

        return chain.proceed(message)
    }

}
