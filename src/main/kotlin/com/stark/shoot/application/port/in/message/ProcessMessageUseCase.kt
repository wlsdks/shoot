package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.chat.message.ChatMessage

interface ProcessMessageUseCase {
    fun processMessage(message: ChatMessage): ChatMessage
    fun markMessageAsRead(messageId: String, userId: String): ChatMessage // 메시지 단위 읽음
    fun markAllMessagesAsRead(roomId: String, userId: String) // 방 전체 읽음 (원래 MessageReadUseCase에서 옮김)
}