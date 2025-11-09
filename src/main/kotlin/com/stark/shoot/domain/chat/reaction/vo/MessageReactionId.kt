package com.stark.shoot.domain.chat.reaction.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

@ValueObject
@JvmInline
value class MessageReactionId(val value: Long) {
    companion object {
        fun from(value: Long): MessageReactionId = MessageReactionId(value)
    }
}
