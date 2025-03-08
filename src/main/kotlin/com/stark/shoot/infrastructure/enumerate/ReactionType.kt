package com.stark.shoot.infrastructure.enumerate

enum class ReactionType(
    val code: String,
    val emoji: String,
    val description: String
) {
    LIKE("like", "👍", "좋아요"),
    SAD("sad", "😢", "슬퍼요"),
    DISLIKE("dislike", "👎", "싫어요"),
    ANGRY("angry", "😡", "화나요"),
    CURIOUS("curious", "🤔", "궁금해요"),
    SURPRISED("surprised", "😮", "놀라워요");

    companion object {
        /**
         * 코드로 리액션 타입 반환
         */
        fun fromCode(code: String): ReactionType? {
            return entries.find { it.code == code }
        }

        /**
         * 이모지로 리액션 타입 반환
         */
        fun fromEmoji(emoji: String): ReactionType? {
            return entries.find { it.emoji == emoji }
        }

        /**
         * 모든 리액션 타입 정보 반환
         */
        fun getAllReactionTypes(): List<Map<String, String>> {
            return entries.map {
                mapOf(
                    "code" to it.code,
                    "emoji" to it.emoji,
                    "description" to it.description
                )
            }
        }
    }

}