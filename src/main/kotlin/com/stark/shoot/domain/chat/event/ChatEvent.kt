package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.chat.message.ChatMessage
import java.time.Instant

data class ChatEvent(
    val version: String = "1.0",
    val type: EventType,
    val data: ChatMessage,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        /**
         * ChatMessage로부터 ChatEvent 객체를 생성합니다.
         *
         * @param message ChatMessage
         * @param eventType 이벤트 타입 (기본값: MESSAGE_CREATED)
         * @return ChatEvent
         */
        fun fromMessage(
            message: ChatMessage,
            eventType: EventType = EventType.MESSAGE_CREATED
        ): ChatEvent {
            return ChatEvent(
                type = eventType,
                data = message
            )
        }
    }
}
