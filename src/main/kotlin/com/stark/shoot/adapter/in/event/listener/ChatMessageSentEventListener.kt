package com.stark.shoot.adapter.`in`.event.listener

import com.stark.shoot.domain.chat.event.ChatMessageSentEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * ChatMessageSentEventListener는 입력 어댑터(Inbound Adapter)입니다.
 * 외부(이벤트 시스템)에서 애플리케이션 코어로 이벤트를 수신하는 인바운드 어댑터.
 */
@Component
class ChatMessageSentEventListener {

    @EventListener
    fun handle(event: ChatMessageSentEvent) {
        // 예: 메시지 알림 로직 구현
        println("새 메시지 전송됨: ${event.chatMessage.content}")
        // 실제로는 이메일, 푸시 알림 등 다양한 방식으로 구현 가능
    }

}