package com.stark.shoot.adapter.`in`.event.listener

import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * 채팅방의 unreadCount 업데이트 이벤트를 수신하여 각 사용자에게 WebSocket 메시지로 전파합니다.
 * 이 메서드는 이벤트에 포함된 unreadCount 정보를 읽어 각 사용자 ID에 대해 목적지를 구성합니다.
 */
@Component
class ChatUnreadCountUpdatedEventListener(
    private val messagingTemplate: SimpMessagingTemplate
) {

    // todo: 중요사항! 클라이언트는 자신의 사용자 ID에 해당하는 채널을 구독합니다. (클라이언트 코드 구성이 필요)
    //  서버에서 전송한 메시지를 수신하여, 채팅방 목록이나 알림 뱃지를 실시간으로 갱신합니다.
    @EventListener
    fun handle(event: ChatUnreadCountUpdatedEvent) {
        // 각 참여자별로 업데이트된 unreadCount를 전송
        event.unreadCounts.forEach { (userId, count) ->
            val destination = "/topic/chatrooms/$userId/updates"
            messagingTemplate.convertAndSend(destination, mapOf("roomId" to event.roomId, "unreadCount" to count))
        }
    }

}