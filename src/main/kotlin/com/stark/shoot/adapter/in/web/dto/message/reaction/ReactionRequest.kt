package com.stark.shoot.adapter.`in`.web.dto.message.reaction

data class ReactionRequest(
    val reactionType: String  // ReactionType.code 값 (e.g. "like", "sad")
)