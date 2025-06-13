package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadSummaryDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.thread.GetThreadsUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.toObjectId

@UseCase
class GetThreadsService(
    private val loadMessagePort: LoadMessagePort,
    private val chatMessageMapper: ChatMessageMapper,
) : GetThreadsUseCase {

    override fun getThreads(roomId: Long, lastThreadId: String?, limit: Int): List<ThreadSummaryDto> {
        val rootMessages = if (lastThreadId != null) {
            loadMessagePort.findThreadRootsByRoomIdAndBeforeId(roomId, lastThreadId.toObjectId(), limit)
        } else {
            loadMessagePort.findThreadRootsByRoomId(roomId, limit)
        }

        return rootMessages.map { message ->
            val count = loadMessagePort.countByThreadId(message.id!!.toObjectId())
            ThreadSummaryDto(
                rootMessage = chatMessageMapper.toDto(message),
                replyCount = count
            )
        }
    }
}
