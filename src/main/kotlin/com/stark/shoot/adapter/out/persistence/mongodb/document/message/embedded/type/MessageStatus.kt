package com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type

enum class MessageStatus {
    SENDING,
    PROCESSING,
    SENT_TO_KAFKA,
    SAVED,
    FAILED
}