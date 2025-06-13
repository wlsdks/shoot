package com.stark.shoot.application.port.`in`.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadSummaryDto

interface GetThreadsUseCase {
    fun getThreads(roomId: Long, lastThreadId: String?, limit: Int): List<ThreadSummaryDto>
}
