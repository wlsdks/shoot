package com.stark.shoot.adapter.`in`.web.dto.message.reaction

data class ReactionListResponse(
    val messageId: String,
    val reactions: List<ReactionInfoDto>
) {
    companion object {
        fun from(messageId: String, reactions: Map<String, Set<Long>>): ReactionListResponse {
            val reactionInfos = ReactionInfoDto.fromReactionsMap(reactions)
            return ReactionListResponse(messageId, reactionInfos)
        }
    }
}
