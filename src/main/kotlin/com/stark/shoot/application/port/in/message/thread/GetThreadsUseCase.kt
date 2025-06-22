package com.stark.shoot.application.port.`in`.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadSummaryDto
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId

interface GetThreadsUseCase {
    fun getThreads(roomId: ChatRoomId, lastThreadId: MessageId?, limit: Int): List<ThreadSummaryDto>
}
