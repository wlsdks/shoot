package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.filter.common.MessageProcessingFilter
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