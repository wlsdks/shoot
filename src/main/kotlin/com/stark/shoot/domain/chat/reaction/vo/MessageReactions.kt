package com.stark.shoot.domain.chat.reaction.vo

/**
 * 메시지 반응을 관리하는 값 객체입니다.
 * 이 클래스는 메시지에 대한 반응 추가, 제거, 토글 등의 기능을 제공합니다.
 */
data class MessageReactions(
    val reactions: Map<String, Set<Long>> = emptyMap()
) {
    companion object {
        /**
         * 빈 메시지 반응 객체 생성
         *
         * @return 빈 MessageReactions 객체
         */
        fun create(): MessageReactions {
            return MessageReactions()
        }

        /**
         * 초기 반응이 있는 메시지 반응 객체 생성
         *
         * @param reactionType 반응 타입
         * @param userId 사용자 ID
         * @return 초기 반응이 설정된 MessageReactions 객체
         */
        fun createWithInitialReaction(
            reactionType: String,
            userId: Long
        ): MessageReactions {
            return MessageReactions(
                reactions = mapOf(reactionType to setOf(userId))
            )
        }
    }

    /**
     * 반응 추가
     *
     * @param userId 사용자 ID
     * @param reactionType 반응 타입 (예: "like", "heart", "laugh")
     * @return 업데이트된 MessageReactions 객체
     */
    fun addReaction(
        userId: Long,
        reactionType: String
    ): MessageReactions {
        val updatedReactions = this.reactions.toMutableMap()

        // 해당 반응 타입에 대한 사용자 목록 가져오기 또는 새로 생성
        val usersWithReaction = updatedReactions[reactionType]?.toMutableSet() ?: mutableSetOf()

        // 사용자 ID 추가
        usersWithReaction.add(userId)

        // 업데이트된 사용자 목록 저장
        updatedReactions[reactionType] = usersWithReaction

        return this.copy(reactions = updatedReactions)
    }

    /**
     * 반응 제거
     *
     * @param userId 사용자 ID
     * @param reactionType 반응 타입 (예: "like", "heart", "laugh")
     * @return 업데이트된 MessageReactions 객체
     */
    fun removeReaction(
        userId: Long,
        reactionType: String
    ): MessageReactions {
        val updatedReactions = this.reactions.toMutableMap()

        // 해당 반응 타입에 대한 사용자 목록이 없으면 변경 없음
        val usersWithReaction = updatedReactions[reactionType]?.toMutableSet() ?: return this

        // 사용자 ID 제거
        usersWithReaction.remove(userId)

        // 사용자 목록이 비어있으면 해당 반응 타입 자체를 제거
        if (usersWithReaction.isEmpty()) {
            updatedReactions.remove(reactionType)
        } else {
            updatedReactions[reactionType] = usersWithReaction
        }

        return this.copy(reactions = updatedReactions)
    }

    /**
     * 사용자가 이미 추가한 리액션 타입을 찾습니다.
     *
     * @param userId 사용자 ID
     * @return 사용자가 추가한 리액션 타입 코드 또는 null
     */
    fun findUserExistingReactionType(userId: Long): String? {
        return reactions.entries
            .find { (_, users) -> userId in users }
            ?.key
    }

}
