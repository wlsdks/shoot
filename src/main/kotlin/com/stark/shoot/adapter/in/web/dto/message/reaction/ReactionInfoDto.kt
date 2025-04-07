package com.stark.shoot.adapter.`in`.web.dto.message.reaction

data class ReactionInfoDto(
    val reactionType: String,     // 리액션 타입 코드
    val emoji: String,            // 이모지
    val description: String,      // 설명
    val userIds: List<Long>,    // 리액션한 사용자 ID 목록
    val count: Int                // 리액션 수
)