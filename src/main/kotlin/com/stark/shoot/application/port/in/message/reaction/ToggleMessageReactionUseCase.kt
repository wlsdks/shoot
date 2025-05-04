package com.stark.shoot.application.port.`in`.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse

interface ToggleMessageReactionUseCase {
    fun toggleReaction(
        messageId: String,
        userId: Long,
        reactionType: String
    ): ReactionResponse
}
