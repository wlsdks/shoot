package com.stark.shoot.application.port.out

import com.stark.shoot.domain.chat.message.ChatMessage

interface SaveChatMessagePort {

    fun save(message: ChatMessage): ChatMessage
    fun saveAll(messages: List<ChatMessage>): List<ChatMessage>

}