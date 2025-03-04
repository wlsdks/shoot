package com.stark.shoot.adapter.`in`.web.dto.message.reaction

import com.stark.shoot.domain.chat.message.ChatMessage

data class ReactionResponse(
    val messageId: String,
    val reactions: Map<String, Set<String>>,
    val updatedAt: String
) {
    companion object {
        fun from(message: ChatMessage): ReactionResponse {
            return ReactionResponse(
                messageId = message.id ?: "",
                reactions = message.reactions,
                updatedAt = message.updatedAt?.toString() ?: ""
            )
        }
    }
}
