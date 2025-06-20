package com.stark.shoot.domain.chat.message.vo

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.reaction.vo.MessageReactions
import com.stark.shoot.domain.user.vo.UserId

/**
 * 리액션 토글 결과를 나타내는 데이터 클래스
 */
data class ReactionToggleResult(
    val reactions: MessageReactions,
    val message: ChatMessage,
    val userId: UserId,
    val reactionType: String,
    val isAdded: Boolean,
    val previousReactionType: String? = null,
    val isReplacement: Boolean = false
)