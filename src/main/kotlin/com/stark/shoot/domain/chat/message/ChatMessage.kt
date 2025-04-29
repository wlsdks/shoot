package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
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

    companion object {
        /**
         * ChatMessageRequest로부터 ChatMessage 객체를 생성합니다.
         *
         * @param request ChatMessageRequest
         * @return ChatMessage
         */
        fun fromRequest(request: ChatMessageRequest): ChatMessage {
            val chatMessage = ChatMessage(
                roomId = request.roomId,
                senderId = request.senderId,
                content = MessageContent(
                    text = request.content.text,
                    type = MessageType.TEXT
                ),
                status = MessageStatus.SAVED,
                createdAt = Instant.now()
            )

            // 메타데이터 복사 (tempId와 status 포함)
            if (request.metadata != null) {
                chatMessage.metadata = chatMessage.metadata.requestToDomain(request.metadata)
            }

            return chatMessage
        }

        /**
         * URL 미리보기를 처리합니다.
         * 메시지의 content가 TEXT 타입일 때만 URL을 추출합니다.
         *
         * @param request 메시지 요청
         * @param extractUrlPort URL 추출 포트
         * @param cacheUrlPreviewPort URL 미리보기 캐시 포트
         * @return 업데이트된 메시지 요청
         */
        fun processUrlPreview(
            request: ChatMessageRequest,
            extractUrlPort: com.stark.shoot.application.port.out.message.preview.ExtractUrlPort,
            cacheUrlPreviewPort: com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
        ): ChatMessageRequest {
            if (request.content.type == MessageType.TEXT) {
                val urls = extractUrlPort.extractUrls(request.content.text)
                if (urls.isNotEmpty()) {
                    val url = urls.first()
                    val cachedPreview = cacheUrlPreviewPort.getCachedUrlPreview(url)

                    // 캐시된 미리보기가 있으면 메시지에 추가
                    if (cachedPreview != null) {
                        request.metadata.urlPreview = cachedPreview
                    } else {
                        // 캐시 미스인 경우 처리 필요 표시
                        request.metadata.needsUrlPreview = true
                        request.metadata.previewUrl = url
                    }
                }
            }
            return request
        }
    }
}
