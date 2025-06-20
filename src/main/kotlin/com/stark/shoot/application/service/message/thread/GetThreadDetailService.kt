package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadDetailDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.thread.GetThreadDetailUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.LoadThreadPort
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class GetThreadDetailService(
    private val loadMessagePort: LoadMessagePort,
    private val loadThreadPort: LoadThreadPort,
    private val chatMessageMapper: ChatMessageMapper,
) : GetThreadDetailUseCase {

    override fun getThreadDetail(
        threadId: MessageId,
        lastMessageId: MessageId?,
        limit: Int
    ): ThreadDetailDto {
        val rootMessage = loadMessagePort.findById(threadId)
            ?: throw ResourceNotFoundException("스레드 루트 메시지를 찾을 수 없습니다: threadId=$threadId")

        val messages = if (lastMessageId != null) {
            loadThreadPort.findByThreadIdAndBeforeId(threadId, lastMessageId, limit)
        } else {
            loadThreadPort.findByThreadId(threadId, limit)
        }

        return ThreadDetailDto(
            rootMessage = chatMessageMapper.toDto(rootMessage),
            messages = chatMessageMapper.toDtoList(messages)
        )
    }

}
