package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadDetailDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.thread.GetThreadDetailUseCase
import com.stark.shoot.application.port.`in`.message.thread.command.GetThreadDetailCommand
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.thread.ThreadQueryPort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class GetThreadDetailService(
    private val messageQueryPort: MessageQueryPort,
    private val threadQueryPort: ThreadQueryPort,
    private val chatMessageMapper: ChatMessageMapper,
) : GetThreadDetailUseCase {

    override fun getThreadDetail(command: GetThreadDetailCommand): ThreadDetailDto {
        val rootMessage = messageQueryPort.findById(command.threadId)
            ?: throw ResourceNotFoundException("스레드 루트 메시지를 찾을 수 없습니다: threadId=${command.threadId}")

        val messages = if (command.lastMessageId != null) {
            threadQueryPort.findByThreadIdAndBeforeId(command.threadId, command.lastMessageId, command.limit)
        } else {
            threadQueryPort.findByThreadId(command.threadId, command.limit)
        }

        return ThreadDetailDto(
            rootMessage = chatMessageMapper.toDto(rootMessage),
            messages = chatMessageMapper.toDtoList(messages)
        )
    }

}
