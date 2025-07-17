package com.stark.shoot.adapter.`in`.event.chatroom

import com.stark.shoot.adapter.`in`.web.socket.dto.chatroom.ChatRoomUpdateDto
import com.stark.shoot.domain.event.ChatRoomUpdateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class ChatRoomUpdateEventListener(
    private val redisTemplate: StringRedisTemplate,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = KotlinLogging.logger {}

    @EventListener
    fun handleChatRoomUpdate(event: ChatRoomUpdateEvent) {
        val roomId = event.roomId.value
        event.updates.forEach { (userId, updateInfo) ->
            val activeKey = "active:${userId.value}:$roomId"
            val isActive = redisTemplate.opsForValue().get(activeKey) == "true"
            if (!isActive) {
                val update = ChatRoomUpdateDto(
                    roomId = roomId,
                    unreadCount = updateInfo.unreadCount,
                    lastMessage = updateInfo.lastMessage
                )
                messagingTemplate.convertAndSendToUser(
                    userId.value.toString(),
                    "/queue/room-update",
                    update
                )
            } else {
                logger.debug { "User ${userId.value} active in room $roomId, skip update" }
            }
        }
    }
}
