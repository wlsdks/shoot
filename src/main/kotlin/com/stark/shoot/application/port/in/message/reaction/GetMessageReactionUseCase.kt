package com.stark.shoot.application.port.`in`.message.reaction

import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chat.message.vo.MessageId

interface GetMessageReactionUseCase {
    fun getReactions(messageId: MessageId): Map<String, Set<Long>>
    fun getSupportedReactionTypes(): List<ReactionType>
}
