package com.stark.shoot.application.port.`in`.message.reaction

import com.stark.shoot.domain.chat.reaction.ReactionType

interface GetMessageReactionUseCase {
    fun getReactions(messageId: String): Map<String, Set<Long>>
    fun getSupportedReactionTypes(): List<ReactionType>
}
