package com.stark.shoot.domain.chat.message.vo

import com.stark.shoot.domain.chat.message.ChatMessage

/**
 * 메시지 고정 작업의 결과를 나타내는 데이터 클래스
 */
data class PinMessageResult(
    val pinnedMessage: ChatMessage,
    val unpinnedMessage: ChatMessage?
)