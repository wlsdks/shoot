package com.stark.shoot.application.filter.message.chain

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.domain.chat.message.ChatMessage
import org.springframework.stereotype.Component

@Component
class DefaultMessageProcessingChain(
    private val filters: List<MessageProcessingFilter>
) : MessageProcessingChain {

    // 필터 인덱스
    private var index = 0

    override suspend fun proceed(
        message: ChatMessage
    ): ChatMessage {
        // 모든 필터 실행 완료
        if (index >= filters.size) {
            return message
        }

        // 현재 필터 가져오기
        val filter = filters[index++]

        // 필터 실행 (자신을 다음 필터로 전달)
        return filter.process(message, this)
    }

    // 체인 초기화 (재사용)
    fun reset(): DefaultMessageProcessingChain {
        index = 0
        return this
    }

}