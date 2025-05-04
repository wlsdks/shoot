package com.stark.shoot.application.port.`in`.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.infrastructure.enumerate.ReactionType

interface MessageReactionUseCase {
    fun toggleReaction(
        messageId: String,
        userId: Long,
        reactionType: String
    ): ReactionResponse

    fun getReactions(messageId: String): Map<String, Set<Long>>
    fun getSupportedReactionTypes(): List<ReactionType>
}
