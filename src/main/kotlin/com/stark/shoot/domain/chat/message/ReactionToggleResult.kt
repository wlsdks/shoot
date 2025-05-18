package com.stark.shoot.domain.chat.message

import com.stark.shoot.domain.chat.reaction.MessageReactions

/**
 * 리액션 토글 결과를 나타내는 데이터 클래스
 */
data class ReactionToggleResult(
    val reactions: MessageReactions,
    val message: ChatMessage,
    val userId: Long,
    val reactionType: String,
    val isAdded: Boolean,
    val previousReactionType: String? = null,
    val isReplacement: Boolean = false
)