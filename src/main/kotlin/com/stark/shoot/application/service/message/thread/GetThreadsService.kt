package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadSummaryDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.thread.GetThreadsUseCase
import com.stark.shoot.application.port.out.message.thread.ThreadQueryPort
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class GetThreadsService(
    private val threadQueryPort: ThreadQueryPort,
    private val chatMessageMapper: ChatMessageMapper,
) : GetThreadsUseCase {

    override fun getThreads(
        roomId: ChatRoomId,
        lastThreadId: MessageId?,
        limit: Int
    ): List<ThreadSummaryDto> {
        val rootMessages = if (lastThreadId != null) {
            threadQueryPort.findThreadRootsByRoomIdAndBeforeId(roomId, lastThreadId, limit)
        } else {
            threadQueryPort.findThreadRootsByRoomId(roomId, limit)
        }

        return rootMessages.map { message ->
            val count = threadQueryPort.countByThreadId(message.id!!)
            ThreadSummaryDto(
                rootMessage = chatMessageMapper.toDto(message),
                replyCount = count
            )
        }
    }

}
