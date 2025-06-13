package com.stark.shoot.application.port.`in`.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto

interface GetThreadMessagesUseCase {
    fun getThreadMessages(threadId: String, lastMessageId: String?, limit: Int): List<MessageResponseDto>
}
