package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.rest.dto.message.thread.ThreadSummaryDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.thread.GetThreadsUseCase
import com.stark.shoot.application.port.`in`.message.thread.command.GetThreadsCommand
import com.stark.shoot.application.port.out.message.thread.ThreadQueryPort
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class GetThreadsService(
    private val threadQueryPort: ThreadQueryPort,
    private val chatMessageMapper: ChatMessageMapper,
) : GetThreadsUseCase {

    override fun getThreads(
        command: GetThreadsCommand
    ): List<ThreadSummaryDto> {
        val roomId = command.roomId
        val lastThreadId = command.lastThreadId
        val limit = command.limit

        val rootMessages = if (lastThreadId != null) {
            threadQueryPort.findThreadRootsByRoomIdAndBeforeId(roomId, lastThreadId, limit)
        } else {
            threadQueryPort.findThreadRootsByRoomId(roomId, limit)
        }

        // N+1 문제 해결: 모든 스레드 ID에 대한 답글 수를 배치로 조회
        val threadIds = rootMessages.mapNotNull { it.id }
        val replyCounts = if (threadIds.isNotEmpty()) {
            threadQueryPort.countByThreadIds(threadIds)
        } else {
            emptyMap()
        }

        return rootMessages.map { message ->
            val count = replyCounts[message.id!!] ?: 0L
            ThreadSummaryDto(
                rootMessage = chatMessageMapper.toDto(message),
                replyCount = count
            )
        }
    }

}
