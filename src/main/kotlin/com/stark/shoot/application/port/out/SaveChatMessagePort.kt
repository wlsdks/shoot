package com.stark.shoot.application.port.out

interface SaveChatMessagePort {

    fun save(message: ChatMessage): ChatMessage
    fun saveAll(messages: List<ChatMessage>): List<ChatMessage>

}