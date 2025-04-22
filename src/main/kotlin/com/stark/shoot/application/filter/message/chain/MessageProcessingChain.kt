package com.stark.shoot.application.filter.message.chain

import com.stark.shoot.domain.chat.message.ChatMessage

interface MessageProcessingChain {
    /**
     * 다음 필터로 메시지 처리를 진행합니다.
     * 
     * @param message 처리할 메시지
     * @return 처리된 메시지
     */
    suspend fun proceed(message: ChatMessage): ChatMessage

    /**
     * 필터 간 공유 컨텍스트에서 값을 가져옵니다.
     * 
     * @param key 컨텍스트 키
     * @return 컨텍스트 값 또는 null
     */
    fun <T> getFromContext(key: String): T?

    /**
     * 필터 간 공유 컨텍스트에 값을 저장합니다.
     * 
     * @param key 컨텍스트 키
     * @param value 저장할 값
     */
    fun <T> putInContext(key: String, value: T)
}
