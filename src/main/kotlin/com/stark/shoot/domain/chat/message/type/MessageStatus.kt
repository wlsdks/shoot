package com.stark.shoot.domain.chat.message.type

enum class MessageStatus {
    SENDING,
    PROCESSING,
    SENT_TO_KAFKA,
    SAVED,
    FAILED
}
