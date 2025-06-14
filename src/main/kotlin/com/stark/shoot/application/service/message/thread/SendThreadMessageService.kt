package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.application.port.`in`.message.thread.SendThreadMessageUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId

@UseCase
class SendThreadMessageService(
    private val loadMessagePort: LoadMessagePort,
    private val sendMessageUseCase: SendMessageUseCase,
) : SendThreadMessageUseCase {

    override fun sendThreadMessage(request: ChatMessageRequest) {
        val threadId = request.threadId
            ?: throw IllegalArgumentException("threadId must not be null")

        loadMessagePort.findById(threadId.toObjectId())
            ?: throw ResourceNotFoundException("스레드 루트 메시지를 찾을 수 없습니다: threadId=$threadId")

        sendMessageUseCase.sendMessage(request)
    }

}
