package com.stark.shoot.domain.event

import com.stark.shoot.domain.chat.message.ChatMessage

data class MessageSendedEvent(
    val message: ChatMessage,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        fun create(message: ChatMessage): MessageSendedEvent {
            return MessageSendedEvent(message)
        }
    }
}
