package com.stark.shoot.adapter.`in`.web.dto.message.reaction

import com.stark.shoot.infrastructure.enumerate.ReactionType

data class ReactionListResponse(
    val messageId: String,
    val reactions: List<ReactionInfoDto>
) {
    companion object {
        fun from(messageId: String, reactions: Map<String, Set<String>>): ReactionListResponse {
            val reactionInfos = reactions.map { (reactionType, userIds) ->
                val type = ReactionType.fromCode(reactionType)
                    ?: ReactionType.LIKE // 기본값

                ReactionInfoDto(
                    reactionType = type.code,
                    emoji = type.emoji,
                    description = type.description,
                    userIds = userIds.toList(),
                    count = userIds.size
                )
            }

            return ReactionListResponse(messageId, reactionInfos)
        }
    }
}