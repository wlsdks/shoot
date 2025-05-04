package com.stark.shoot.adapter.`in`.web.dto.message.reaction

data class ReactionResponse(
    val messageId: String,
    val reactions: List<ReactionInfoDto>,
    val updatedAt: String
) {
    companion object {
        fun from(
            messageId: String,
            reactions: Map<String, Set<Long>>,
            updatedAt: String
        ): ReactionResponse {
            val reactionInfos = ReactionInfoDto.fromReactionsMap(reactions)
            return ReactionResponse(messageId, reactionInfos, updatedAt)
        }
    }
}
