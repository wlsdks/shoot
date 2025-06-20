package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.thread.GetThreadMessagesUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class GetThreadMessagesService(
    private val loadMessagePort: LoadMessagePort,
    private val chatMessageMapper: ChatMessageMapper,
) : GetThreadMessagesUseCase {

    override fun getThreadMessages(
        threadId: MessageId,
        lastMessageId: MessageId?,
        limit: Int
    ): List<MessageResponseDto> {
        val messages = if (lastMessageId != null) {
            loadMessagePort.findByThreadIdAndBeforeId(threadId, lastMessageId, limit)
        } else {
            loadMessagePort.findByThreadId(threadId, limit)
        }

        return chatMessageMapper.toDtoList(messages)
    }

}
