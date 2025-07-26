package com.stark.shoot.application.port.`in`.message.reaction

import com.stark.shoot.adapter.`in`.rest.dto.message.reaction.ReactionResponse
import com.stark.shoot.application.port.`in`.message.reaction.command.ToggleMessageReactionCommand

interface ToggleMessageReactionUseCase {
    fun toggleReaction(command: ToggleMessageReactionCommand): ReactionResponse
}
