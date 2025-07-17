package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.ChatRoomUpdateNotifyUseCase
import com.stark.shoot.application.port.out.chatroom.SendChatRoomUpdatePort
import com.stark.shoot.domain.event.ChatRoomUpdateEvent
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate

@UseCase
class ChatRoomUpdateNotifyService(
    private val redisTemplate: StringRedisTemplate,
    private val sendChatRoomUpdatePort: SendChatRoomUpdatePort
) : ChatRoomUpdateNotifyUseCase {
    private val logger = KotlinLogging.logger {}

    override fun notify(event: ChatRoomUpdateEvent) {
        val roomId = event.roomId
        
        event.updates.forEach { (userId, updateInfo) ->
            val activeKey = "active:${userId.value}:${roomId.value}"
            val isActive = redisTemplate.opsForValue().get(activeKey) == "true"

            if (!isActive) {
                sendChatRoomUpdatePort.sendUpdate(userId, roomId, updateInfo)
            } else {
                logger.debug { "User ${userId.value} active in room ${roomId.value}, skip update" }
            }
        }
    }

}
