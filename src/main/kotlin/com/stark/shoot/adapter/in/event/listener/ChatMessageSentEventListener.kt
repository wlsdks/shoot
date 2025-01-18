package com.stark.shoot.adapter.`in`.event.listener

import com.stark.shoot.adapter.`in`.websocket.ChatWebSocketHandler
import com.stark.shoot.domain.chat.event.ChatMessageSentEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * ChatMessageSentEventListener는 입력 어댑터(Inbound Adapter)입니다.
 * 외부(이벤트 시스템)에서 애플리케이션 코어로 이벤트를 수신하는 인바운드 어댑터.
 */
@Component
class ChatMessageSentEventListener(
    private val webSocketHandler: ChatWebSocketHandler
) {

    @EventListener
    fun handle(event: ChatMessageSentEvent) {
        println("ChatMessageSentEvent 수신: ${event.chatMessage}")
//        webSocketHandler.broadcastMessage(event.chatMessage)
    }

}