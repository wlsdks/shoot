package com.stark.shoot.application.port.`in`.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.domain.common.vo.MessageId

interface GetThreadMessagesUseCase {
    fun getThreadMessages(threadId: MessageId, lastMessageId: MessageId?, limit: Int): List<MessageResponseDto>
}
