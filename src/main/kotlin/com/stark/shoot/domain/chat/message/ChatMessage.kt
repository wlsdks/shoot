package com.stark.shoot.domain.chat.message

import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.reaction.MessageReactions
import com.stark.shoot.domain.chat.reaction.ReactionType
import com.stark.shoot.domain.common.vo.MessageId
import java.time.Instant

data class ChatMessage(
    val id: MessageId? = null,
    val roomId: Long,
    val senderId: Long,
    val content: MessageContent,
    val status: MessageStatus,
    val replyToMessageId: MessageId? = null,
    val threadId: MessageId? = null,
    val expiresAt: Instant? = null,
    val messageReactions: MessageReactions = MessageReactions(),
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
     * 메시지의 반응 맵을 반환합니다.
     * 이는 하위 호환성을 위한 속성입니다.
     */
    val reactions: Map<String, Set<Long>>
        get() = messageReactions.reactions

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
     * 메시지가 만료되었는지 확인합니다.
     *
     * @param now 기준 시간 (기본값: 현재 시간)
     * @return 만료 여부
     */
    fun isExpired(now: Instant = Instant.now()): Boolean {
        return expiresAt?.isBefore(now) ?: false
    }

    /**
     * 메시지 만료 시간을 설정합니다.
     *
     * @param instant 만료 시각
     * @return 업데이트된 ChatMessage 객체
     */
    fun setExpiration(instant: Instant?): ChatMessage {
        return this.copy(expiresAt = instant, updatedAt = Instant.now())
    }

    /**
     * 메시지 고정 상태 변경
     *
     * @param isPinned 고정 여부
     * @param userId 고정/해제한 사용자 ID (고정 시에만 사용)
     * @return 업데이트된 ChatMessage 객체
     */
    fun updatePinStatus(
        isPinned: Boolean,
        userId: Long? = null
    ): ChatMessage {
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
     * 채팅방에서 메시지를 고정합니다.
     * 이 메서드는 도메인 규칙 "한 채팅방에는 최대 하나의 고정 메시지만 존재할 수 있다"를 강제합니다.
     *
     * @param userId 고정한 사용자 ID
     * @param currentPinnedMessage 채팅방에 현재 고정된 메시지 (있는 경우)
     * @return 고정 작업 결과 (고정할 메시지와 해제할 메시지)
     */
    fun pinMessageInRoom(
        userId: Long,
        currentPinnedMessage: ChatMessage?
    ): PinMessageResult {
        // 이미 고정된 메시지인지 확인
        if (this.isPinned) {
            return PinMessageResult(this, null)
        }

        // 새 메시지 고정
        val pinnedMessage = this.updatePinStatus(true, userId)

        // 기존 고정 메시지가 있으면 해제 정보 반환
        val messageToUnpin = currentPinnedMessage?.updatePinStatus(false)

        return PinMessageResult(pinnedMessage, messageToUnpin)
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
    fun toggleReaction(
        userId: Long,
        reactionType: ReactionType
    ): ReactionToggleResult {
        // 사용자가 이미 추가한 리액션 타입 찾기
        val userExistingReactionType = messageReactions.findUserExistingReactionType(userId)

        // 토글 처리 결과 변수
        val updatedReactions: MessageReactions
        val isAdded: Boolean
        val previousReactionType: String?
        val isReplacement: Boolean

        // 토글 처리
        when {
            // 1. 같은 리액션을 선택한 경우: 제거
            userExistingReactionType == reactionType.code -> {
                updatedReactions = messageReactions.removeReaction(userId, reactionType.code)
                isAdded = false
                previousReactionType = null
                isReplacement = false
            }

            // 2. 다른 리액션이 이미 있는 경우: 기존 리액션 제거 후 새 리액션 추가
            userExistingReactionType != null -> {
                val reactionsAfterRemove = messageReactions.removeReaction(userId, userExistingReactionType)
                updatedReactions = reactionsAfterRemove.addReaction(userId, reactionType.code)
                isAdded = true
                previousReactionType = userExistingReactionType
                isReplacement = true
            }

            // 3. 리액션이 없는 경우: 새 리액션 추가
            else -> {
                updatedReactions = messageReactions.addReaction(userId, reactionType.code)
                isAdded = true
                previousReactionType = null
                isReplacement = false
            }
        }

        // 업데이트된 메시지 생성
        val updatedMessage = this.copy(
            messageReactions = updatedReactions,
            updatedAt = Instant.now()
        )

        // ReactionToggleResult 반환
        return ReactionToggleResult(
            reactions = updatedReactions,
            message = updatedMessage,
            userId = userId,
            reactionType = reactionType.code,
            isAdded = isAdded,
            previousReactionType = previousReactionType,
            isReplacement = isReplacement
        )
    }

    /**
     * URL 미리보기 정보를 설정합니다.
     *
     * @param urlPreview URL 미리보기 정보
     * @return 업데이트된 ChatMessage 객체
     */
    fun setUrlPreview(urlPreview: UrlPreview): ChatMessage {
        val updatedMetadata = this.metadata.copy(
            urlPreview = urlPreview,
            needsUrlPreview = false
        )

        return this.copy(
            metadata = updatedMetadata,
            updatedAt = Instant.now()
        )
    }

    /**
     * URL 미리보기가 필요함을 표시합니다.
     *
     * @param url 미리보기가 필요한 URL
     * @return 업데이트된 ChatMessage 객체
     */
    fun markNeedsUrlPreview(url: String): ChatMessage {
        val updatedMetadata = this.metadata.copy(
            needsUrlPreview = true,
            previewUrl = url
        )

        return this.copy(
            metadata = updatedMetadata,
            updatedAt = Instant.now()
        )
    }

    companion object {

        /**
         * 새 메시지를 생성합니다.
         *
         * @param roomId 채팅방 ID
         * @param senderId 발신자 ID
         * @param text 메시지 텍스트
         * @param type 메시지 타입
         * @param tempId 임시 ID (선택)
         * @return 생성된 ChatMessage 객체
         */
        fun create(
            roomId: Long,
            senderId: Long,
            text: String,
            type: MessageType = MessageType.TEXT,
            tempId: String? = null,
            threadId: MessageId? = null,
            expiresAt: Instant? = null
        ): ChatMessage {
            val content = MessageContent(
                text = text,
                type = type
            )

            val metadata = ChatMessageMetadata(
                tempId = tempId
            )

            return ChatMessage(
                roomId = roomId,
                senderId = senderId,
                content = content,
                status = MessageStatus.SENDING,
                metadata = metadata,
                threadId = threadId,
                expiresAt = expiresAt
            )
        }

        /**
         * 메시지에서 URL을 추출하고 미리보기 정보를 설정합니다.
         *
         * @param message 메시지
         * @param extractUrls URL 추출 함수
         * @param getCachedPreview 캐시된 미리보기 조회 함수
         * @return 업데이트된 ChatMessage 객체
         */
        fun processUrlPreview(
            message: ChatMessage,
            extractUrls: (String) -> List<String>,
            getCachedPreview: (String) -> UrlPreview?
        ): ChatMessage {
            // 텍스트 메시지가 아니면 처리하지 않음
            if (message.content.type != MessageType.TEXT) {
                return message
            }

            // URL 추출
            val urls = extractUrls(message.content.text)
            if (urls.isEmpty()) {
                return message
            }

            // 첫 번째 URL에 대한 미리보기 처리
            val url = urls.first()
            val cachedPreview = getCachedPreview(url)

            return if (cachedPreview != null) {
                // 캐시된 미리보기가 있으면 설정
                message.setUrlPreview(cachedPreview)
            } else {
                // 캐시된 미리보기가 없으면 필요함을 표시
                message.markNeedsUrlPreview(url)
            }
        }

    }

}
