package com.stark.shoot.application.port.`in`.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadDetailDto

interface GetThreadDetailUseCase {
    fun getThreadDetail(threadId: String, lastMessageId: String?, limit: Int): ThreadDetailDto
}
