package com.stark.shoot.application.port.`in`.message.mark

import com.stark.shoot.domain.chat.message.ChatMessage

interface MarkMessageReadUseCase {
    fun markMessageAsRead(messageId: String, userId: String): ChatMessage // 메시지 단위 읽음
    fun markAllMessagesAsRead(roomId: String, userId: String, requestId: String?)
}