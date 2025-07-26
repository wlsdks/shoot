package com.stark.shoot.adapter.`in`.rest.dto.message.reaction

import com.stark.shoot.domain.chat.reaction.type.ReactionType

data class ReactionInfoDto(
    val reactionType: String,     // 리액션 타입 코드
    val emoji: String,            // 이모지
    val description: String,      // 설명
    val userIds: List<Long>,    // 리액션한 사용자 ID 목록
    val count: Int                // 리액션 수
) {
    companion object {
        /**
         * 리액션 맵을 ReactionInfoDto 리스트로 변환합니다.
         *
         * @param reactions 리액션 맵 (리액션 타입 -> 사용자 ID 집합)
         * @return ReactionInfoDto 리스트
         */
        fun fromReactionsMap(reactions: Map<String, Set<Long>>): List<ReactionInfoDto> {
            return reactions.map { (reactionType, userIds) ->
                val type = ReactionType.fromCode(reactionType)
                    ?: ReactionType.LIKE // 기본값

                ReactionInfoDto(
                    reactionType = type.code,
                    emoji = type.emoji,
                    description = type.description,
                    userIds = userIds.toList(),
                    count = userIds.size
                )
            }
        }
    }
}
