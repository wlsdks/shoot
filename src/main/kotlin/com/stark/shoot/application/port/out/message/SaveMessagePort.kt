package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.ChatMessage

interface SaveMessagePort {
    fun save(message: ChatMessage): ChatMessage
    fun saveAll(messages: List<ChatMessage>): List<ChatMessage>
}