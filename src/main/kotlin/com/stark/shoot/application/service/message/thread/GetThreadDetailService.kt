package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadDetailDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.thread.GetThreadDetailUseCase
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.thread.ThreadQueryPort
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class GetThreadDetailService(
    private val messageQueryPort: MessageQueryPort,
    private val threadQueryPort: ThreadQueryPort,
    private val chatMessageMapper: ChatMessageMapper,
) : GetThreadDetailUseCase {

    override fun getThreadDetail(
        threadId: MessageId,
        lastMessageId: MessageId?,
        limit: Int
    ): ThreadDetailDto {
        val rootMessage = messageQueryPort.findById(threadId)
            ?: throw ResourceNotFoundException("스레드 루트 메시지를 찾을 수 없습니다: threadId=$threadId")

        val messages = if (lastMessageId != null) {
            threadQueryPort.findByThreadIdAndBeforeId(threadId, lastMessageId, limit)
        } else {
            threadQueryPort.findByThreadId(threadId, limit)
        }

        return ThreadDetailDto(
            rootMessage = chatMessageMapper.toDto(rootMessage),
            messages = chatMessageMapper.toDtoList(messages)
        )
    }

}
