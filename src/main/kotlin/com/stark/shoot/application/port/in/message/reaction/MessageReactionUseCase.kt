package com.stark.shoot.application.port.`in`.message.reaction

import com.stark.shoot.domain.chat.message.ChatMessage

interface MessageReactionUseCase {
    fun addReaction(messageId: String, userId: String, reactionType: String): ChatMessage
    fun removeReaction(messageId: String, userId: String, reactionType: String): ChatMessage
    fun getReactions(messageId: String): Map<String, Set<String>>
}