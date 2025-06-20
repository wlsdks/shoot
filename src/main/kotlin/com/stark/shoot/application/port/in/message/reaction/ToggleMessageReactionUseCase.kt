package com.stark.shoot.application.port.`in`.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId

interface ToggleMessageReactionUseCase {
    fun toggleReaction(
        messageId: MessageId,
        userId: UserId,
        reactionType: String
    ): ReactionResponse
}
