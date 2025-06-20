package com.stark.shoot.application.port.`in`.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadDetailDto
import com.stark.shoot.domain.chat.message.vo.MessageId

interface GetThreadDetailUseCase {
    fun getThreadDetail(threadId: MessageId, lastMessageId: MessageId?, limit: Int): ThreadDetailDto
}
