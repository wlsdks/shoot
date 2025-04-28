package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import java.time.Instant

data class ChatMessage(
    val id: String? = null,
    val roomId: Long,
    val senderId: Long,
    val content: MessageContent,
    val status: MessageStatus,
    val replyToMessageId: String? = null,
    val reactions: Map<String, Set<Long>> = emptyMap(),
    val mentions: Set<Long> = emptySet(),
    val createdAt: Instant? = Instant.now(),
    val updatedAt: Instant? = null,
    val isDeleted: Boolean = false,
    val readBy: MutableMap<Long, Boolean> = mutableMapOf(),
    var metadata: ChatMessageMetadata = ChatMessageMetadata(),

    // 메시지 고정기능
    val isPinned: Boolean = false,
    val pinnedBy: Long? = null,
    val pinnedAt: Instant? = null
) {
    /**
     * 메시지 읽음 상태 업데이트
     *
     * @param userId 사용자 ID
     * @return 업데이트된 ChatMessage 객체
     */
    fun markAsRead(userId: Long): ChatMessage {
        val updatedReadBy = this.readBy.toMutableMap()
        updatedReadBy[userId] = true

        return this.copy(
            readBy = updatedReadBy,
            metadata = this.metadata.copy(readAt = Instant.now())
        )
    }

    /**
     * 메시지에 반응 추가
     *
     * @param userId 사용자 ID
     * @param reactionType 반응 타입 (예: "like", "heart", "laugh")
     * @return 업데이트된 ChatMessage 객체
     */
    fun addReaction(userId: Long, reactionType: String): ChatMessage {
        val updatedReactions = this.reactions.toMutableMap()

        // 해당 반응 타입에 대한 사용자 목록 가져오기 또는 새로 생성
        val usersWithReaction = updatedReactions[reactionType]?.toMutableSet() ?: mutableSetOf()

        // 사용자 ID 추가
        usersWithReaction.add(userId)

        // 업데이트된 사용자 목록 저장
        updatedReactions[reactionType] = usersWithReaction

        return this.copy(
            reactions = updatedReactions,
            updatedAt = Instant.now()
        )
    }

    /**
     * 메시지에서 반응 제거
     *
     * @param userId 사용자 ID
     * @param reactionType 반응 타입 (예: "like", "heart", "laugh")
     * @return 업데이트된 ChatMessage 객체
     */
    fun removeReaction(userId: Long, reactionType: String): ChatMessage {
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

        return this.copy(
            reactions = updatedReactions,
            updatedAt = Instant.now()
        )
    }

    /**
     * 메시지 고정 상태 변경
     *
     * @param isPinned 고정 여부
     * @param userId 고정/해제한 사용자 ID (고정 시에만 사용)
     * @return 업데이트된 ChatMessage 객체
     */
    fun updatePinStatus(isPinned: Boolean, userId: Long? = null): ChatMessage {
        return if (isPinned) {
            this.copy(
                isPinned = true,
                pinnedBy = userId,
                pinnedAt = Instant.now(),
                updatedAt = Instant.now()
            )
        } else {
            this.copy(
                isPinned = false,
                pinnedBy = null,
                pinnedAt = null,
                updatedAt = Instant.now()
            )
        }
    }
}
