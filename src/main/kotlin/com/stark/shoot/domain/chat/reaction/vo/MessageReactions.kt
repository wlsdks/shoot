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
        fun create(): MessageReactions = MessageReactions()

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
        ): MessageReactions = MessageReactions(
            reactions = mapOf(reactionType to setOf(userId))
        )
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
        val currentUsers = reactions[reactionType] ?: emptySet()
        val updatedUsers = currentUsers + userId
        val updatedReactions = reactions + (reactionType to updatedUsers)
        
        return copy(reactions = updatedReactions)
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
        val currentUsers = reactions[reactionType] ?: return this
        val updatedUsers = currentUsers - userId
        
        val updatedReactions = if (updatedUsers.isEmpty()) {
            reactions - reactionType
        } else {
            reactions + (reactionType to updatedUsers)
        }
        
        return copy(reactions = updatedReactions)
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
