package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.thread.GetThreadMessagesUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.toObjectId

@UseCase
class GetThreadMessagesService(
    private val loadMessagePort: LoadMessagePort,
    private val chatMessageMapper: ChatMessageMapper,
) : GetThreadMessagesUseCase {

    override fun getThreadMessages(threadId: String, lastMessageId: String?, limit: Int): List<MessageResponseDto> {
        val messages = if (lastMessageId != null) {
            loadMessagePort.findByThreadIdAndBeforeId(threadId.toObjectId(), lastMessageId.toObjectId(), limit)
        } else {
            loadMessagePort.findByThreadId(threadId.toObjectId(), limit)
        }
        return chatMessageMapper.toDtoList(messages)
    }
}
