package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.shared.event.type.EventType
import java.time.Instant

data class MessageEvent(
    val version: String = "1.0",
    val type: EventType,
    val data: ChatMessage,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        /**
         * ChatMessage로부터 MessageEvent 객체를 생성합니다.
         *
         * @param message ChatMessage
         * @param eventType 이벤트 타입 (기본값: MESSAGE_CREATED)
         * @return MessageEvent
         */
        fun fromMessage(
            message: ChatMessage,
            eventType: EventType = EventType.MESSAGE_CREATED
        ): MessageEvent {
            return MessageEvent(
                type = eventType,
                data = message
            )
        }
    }
}
