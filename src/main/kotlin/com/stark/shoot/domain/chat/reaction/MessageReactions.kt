package com.stark.shoot.domain.chat.reaction

import java.time.Instant

/**
 * 메시지 반응을 관리하는 값 객체입니다.
 * 이 클래스는 메시지에 대한 반응 추가, 제거, 토글 등의 기능을 제공합니다.
 */
data class MessageReactions(
    val reactions: Map<String, Set<Long>> = emptyMap()
) {
    /**
     * 반응 추가
     *
     * @param userId 사용자 ID
     * @param reactionType 반응 타입 (예: "like", "heart", "laugh")
     * @return 업데이트된 MessageReactions 객체
     */
    fun addReaction(userId: Long, reactionType: String): MessageReactions {
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
    fun removeReaction(userId: Long, reactionType: String): MessageReactions {
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

    /**
     * 메시지에 리액션을 토글합니다.
     * 같은 리액션을 선택하면 제거하고, 다른 리액션을 선택하면 기존 리액션을 제거하고 새 리액션을 추가합니다.
     *
     * @param userId 사용자 ID
     * @param reactionType 리액션 타입
     * @return 토글 결과 (메시지, 기존 리액션 타입, 추가 여부)
     */
    fun toggleReaction(userId: Long, reactionType: ReactionType): ReactionToggleResult {
        // 사용자가 이미 추가한 리액션 타입 찾기
        val userExistingReactionType = findUserExistingReactionType(userId)

        // 토글 처리
        return when {
            // 1. 같은 리액션을 선택한 경우: 제거
            userExistingReactionType == reactionType.code -> {
                val updatedReactions = removeReaction(userId, reactionType.code)
                ReactionToggleResult(updatedReactions, userId, reactionType.code, false)
            }

            // 2. 다른 리액션이 이미 있는 경우: 기존 리액션 제거 후 새 리액션 추가
            userExistingReactionType != null -> {
                val reactionsAfterRemove = removeReaction(userId, userExistingReactionType)
                val reactionsAfterAdd = reactionsAfterRemove.addReaction(userId, reactionType.code)
                ReactionToggleResult(
                    reactions = reactionsAfterAdd, 
                    userId = userId,
                    reactionType = reactionType.code, 
                    isAdded = true, 
                    previousReactionType = userExistingReactionType, 
                    isReplacement = true
                )
            }

            // 3. 리액션이 없는 경우: 새 리액션 추가
            else -> {
                val updatedReactions = addReaction(userId, reactionType.code)
                ReactionToggleResult(updatedReactions, userId, reactionType.code, true)
            }
        }
    }

    /**
     * 리액션 토글 결과를 나타내는 데이터 클래스
     */
    data class ReactionToggleResult(
        val reactions: MessageReactions,
        val userId: Long,
        val reactionType: String,
        val isAdded: Boolean,
        val previousReactionType: String? = null,
        val isReplacement: Boolean = false
    )
}