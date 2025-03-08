package com.stark.shoot.application.port.`in`.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.infrastructure.enumerate.ReactionType

interface MessageReactionUseCase {
    fun addReaction(
        messageId: String,
        userId: String,
        reactionType: String
    ): ReactionResponse

    fun removeReaction(
        messageId: String,
        userId: String,
        reactionType: String
    ): ReactionResponse

    fun getReactions(messageId: String): Map<String, Set<String>>
    fun getSupportedReactionTypes(): List<ReactionType>
}