package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.chat.message.ChatMessage

interface ProcessMessageUseCase {
    // 메시지 저장 및 채팅방 메타데이터 업데이트 담당
    fun processMessage(message: ChatMessage): ChatMessage
}