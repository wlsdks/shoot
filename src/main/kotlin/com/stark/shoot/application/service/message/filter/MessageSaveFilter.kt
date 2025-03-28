package com.stark.shoot.application.service.message.filter

import com.stark.shoot.application.port.`in`.message.process.MessageProcessingChain
import com.stark.shoot.application.port.`in`.message.process.MessageProcessingFilter
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import org.springframework.stereotype.Component

@Component
class MessageSaveFilter(
    private val saveMessagePort: SaveMessagePort
) : MessageProcessingFilter {

    override suspend fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 메시지 저장
        val savedMessage = saveMessagePort.save(message)
        return chain.proceed(savedMessage)
    }

}