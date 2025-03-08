package com.stark.shoot.adapter.`in`.web.dto.message.reaction

import com.stark.shoot.infrastructure.enumerate.ReactionType

data class ReactionResponse(
    val messageId: String,
    val reactions: List<ReactionInfoDto>,
    val updatedAt: String
) {
    companion object {
        fun from(
            messageId: String,
            reactions: Map<String, Set<String>>,
            updatedAt: String
        ): ReactionResponse {
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

            return ReactionResponse(messageId, reactionInfos, updatedAt)
        }
    }
}