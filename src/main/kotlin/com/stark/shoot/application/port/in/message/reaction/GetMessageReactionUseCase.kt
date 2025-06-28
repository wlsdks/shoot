package com.stark.shoot.application.port.`in`.message.reaction

import com.stark.shoot.application.port.`in`.message.reaction.command.GetMessageReactionsCommand
import com.stark.shoot.domain.chat.reaction.type.ReactionType

interface GetMessageReactionUseCase {
    fun getReactions(command: GetMessageReactionsCommand): Map<String, Set<Long>>
    fun getSupportedReactionTypes(): List<ReactionType>
}
