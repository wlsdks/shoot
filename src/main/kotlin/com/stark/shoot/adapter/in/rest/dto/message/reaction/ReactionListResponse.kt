package com.stark.shoot.adapter.`in`.rest.dto.message.reaction

import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
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
