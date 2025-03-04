package com.stark.shoot.adapter.`in`.web.dto.message.reaction

data class ReactionListResponse(
    val messageId: String,
    val reactions: Map<String, Set<String>>
) {
    companion object {
        fun from(messageId: String, reactions: Map<String, Set<String>>): ReactionListResponse {
            return ReactionListResponse(
                messageId = messageId,
                reactions = reactions
            )
        }
    }
}