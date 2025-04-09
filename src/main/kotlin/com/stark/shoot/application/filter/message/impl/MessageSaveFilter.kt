package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
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
        val savedMessage = if (message.id == null) {
            // 새 메시지 저장
            saveMessagePort.save(message)
        } else {
            // 이미 ID가 있는 메시지는 저장하지 않음
            message
        }

        return chain.proceed(savedMessage)
    }

}