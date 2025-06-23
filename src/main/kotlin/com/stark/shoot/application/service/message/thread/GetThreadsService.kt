package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadSummaryDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.thread.GetThreadsUseCase
import com.stark.shoot.application.port.out.message.thread.LoadThreadPort
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class GetThreadsService(
    private val loadThreadPort: LoadThreadPort,
    private val chatMessageMapper: ChatMessageMapper,
) : GetThreadsUseCase {

    override fun getThreads(
        roomId: ChatRoomId,
        lastThreadId: MessageId?,
        limit: Int
    ): List<ThreadSummaryDto> {
        val rootMessages = if (lastThreadId != null) {
            loadThreadPort.findThreadRootsByRoomIdAndBeforeId(roomId, lastThreadId, limit)
        } else {
            loadThreadPort.findThreadRootsByRoomId(roomId, limit)
        }

        return rootMessages.map { message ->
            val count = loadThreadPort.countByThreadId(message.id!!)
            ThreadSummaryDto(
                rootMessage = chatMessageMapper.toDto(message),
                replyCount = count
            )
        }
    }

}
