package com.stark.shoot.adapter.`in`.rest.dto.message.reaction

data class ReactionRequest(
    val messageId: String,      // 웹소켓에서 메시지 ID 식별을 위해 추가
    val reactionType: String,   // ReactionType.code 값 (e.g. "like", "sad")
    val userId: Long           // 웹소켓에서 사용자 식별을 위해 추가
) {
    // REST API 호환성을 위한 별칭
    val reaction: String get() = reactionType
}
