package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.domain.chat.reaction.ReactionType
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

    /**
     * 메시지 내용을 수정합니다.
     * 텍스트 타입의 메시지만 수정 가능합니다.
     *
     * @param newContent 새로운 메시지 내용
     * @return 업데이트된 ChatMessage 객체
     * @throws IllegalArgumentException 메시지 내용이 비어있거나, 이미 삭제된 메시지이거나, 텍스트 타입이 아닌 경우
     */
    fun editMessage(newContent: String): ChatMessage {
        // 내용 유효성 검사
        if (newContent.isBlank()) {
            throw IllegalArgumentException("메시지 내용은 비어있을 수 없습니다.")
        }

        // 삭제된 메시지 확인
        if (this.content.isDeleted) {
            throw IllegalArgumentException("삭제된 메시지는 수정할 수 없습니다.")
        }

        // 메시지 타입 확인 (TEXT 타입만 수정 가능)
        if (this.content.type != MessageType.TEXT) {
            throw IllegalArgumentException("텍스트 타입의 메시지만 수정할 수 있습니다.")
        }

        // 내용 업데이트 및 편집 여부 설정
        val updatedContent = this.content.copy(
            text = newContent,
            isEdited = true
        )

        // 업데이트된 메시지 생성
        return this.copy(
            content = updatedContent,
            updatedAt = Instant.now()
        )
    }

    /**
     * 메시지를 삭제 상태로 변경합니다.
     *
     * @return 업데이트된 ChatMessage 객체
     */
    fun markAsDeleted(): ChatMessage {
        // 삭제 상태로 변경 (isDeleted 플래그 설정)
        val updatedContent = this.content.copy(
            isDeleted = true
        )

        // 업데이트된 메시지 생성
        return this.copy(
            content = updatedContent,
            updatedAt = Instant.now()
        )
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
                val updatedMessage = removeReaction(userId, reactionType.code)
                ReactionToggleResult(updatedMessage, userId, reactionType.code, false)
            }

            // 2. 다른 리액션이 이미 있는 경우: 기존 리액션 제거 후 새 리액션 추가
            userExistingReactionType != null -> {
                val messageAfterRemove = removeReaction(userId, userExistingReactionType)
                val messageAfterAdd = messageAfterRemove.addReaction(userId, reactionType.code)
                ReactionToggleResult(
                    message = messageAfterAdd, 
                    userId = userId,
                    reactionType = reactionType.code, 
                    isAdded = true, 
                    previousReactionType = userExistingReactionType, 
                    isReplacement = true
                )
            }

            // 3. 리액션이 없는 경우: 새 리액션 추가
            else -> {
                val updatedMessage = addReaction(userId, reactionType.code)
                ReactionToggleResult(updatedMessage, userId, reactionType.code, true)
            }
        }
    }

    /**
     * 사용자가 이미 추가한 리액션 타입을 찾습니다.
     *
     * @param userId 사용자 ID
     * @return 사용자가 추가한 리액션 타입 코드 또는 null
     */
    private fun findUserExistingReactionType(userId: Long): String? {
        return reactions.entries
            .find { (_, users) -> userId in users }
            ?.key
    }

    /**
     * 리액션 토글 결과를 나타내는 데이터 클래스
     */
    data class ReactionToggleResult(
        val message: ChatMessage,
        val userId: Long,
        val reactionType: String,
        val isAdded: Boolean,
        val previousReactionType: String? = null,
        val isReplacement: Boolean = false
    )

    companion object {
        // 도메인 로직을 위한 상수나 유틸리티 메서드가 필요하면 여기에 추가

        /**
         * ChatMessageRequest로부터 ChatMessage 객체를 생성합니다.
         *
         * @param request ChatMessageRequest
         * @return ChatMessage
         */
        fun fromRequest(request: ChatMessageRequest): ChatMessage {
            val content = MessageContent(
                text = request.content.text,
                type = request.content.type,
                isEdited = request.content.isEdited,
                isDeleted = request.content.isDeleted,
                attachments = emptyList() // Attachments are handled separately
            )

            val metadata = ChatMessageMetadata(
                tempId = request.metadata.tempId,
                needsUrlPreview = request.metadata.needsUrlPreview,
                previewUrl = request.metadata.previewUrl,
                urlPreview = request.metadata.urlPreview,
                readAt = request.metadata.readAt
            )

            return ChatMessage(
                id = request.id,
                roomId = request.roomId,
                senderId = request.senderId,
                content = content,
                status = request.status ?: MessageStatus.SAVED,
                readBy = request.readBy?.mapKeys { it.key.toLong() }?.toMutableMap() ?: mutableMapOf(),
                metadata = metadata
            )
        }
    }
}
