package com.stark.shoot.domain.chat.message

import com.stark.shoot.domain.chat.exception.MessageException
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.util.TextSanitizer
import com.stark.shoot.domain.chat.message.vo.*
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chat.reaction.vo.MessageReactions
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.AggregateRoot
import java.time.Duration
import java.time.Instant

@AggregateRoot
data class ChatMessage(
    val id: MessageId? = null,
    val roomId: ChatRoomId,
    val senderId: UserId,
    var content: MessageContent,
    var status: MessageStatus,
    var replyToMessageId: MessageId? = null,
    var threadId: MessageId? = null,
    var expiresAt: Instant? = null,
    var mentions: Set<UserId> = emptySet(),
    var createdAt: Instant? = Instant.now(),
    var updatedAt: Instant? = null,
    var metadata: ChatMessageMetadata = ChatMessageMetadata()

    // 메시지 고정 기능은 별도의 MessagePin Aggregate로 분리되었습니다.
    // 메시지 읽음 표시 기능은 별도의 MessageReadReceipt Aggregate로 분리되었습니다.
) {
    /**
     * 메시지 삭제 상태 (하위 호환성용)
     * content.isDeleted로 위임
     */
    val isDeleted: Boolean
        get() = content.isDeleted

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
     */
    fun setExpiration(instant: Instant?) {
        this.expiresAt = instant
        this.updatedAt = Instant.now()
    }

    /**
     * 메시지 내용을 수정합니다.
     * 텍스트 타입의 메시지만 수정 가능합니다.
     * 생성 후 24시간 이내에만 수정할 수 있습니다.
     *
     * @param newContent 새로운 메시지 내용
     * @throws MessageException.EditTimeExpired 메시지 생성 후 24시간이 지난 경우
     * @throws MessageException.EmptyContent 메시지 내용이 비어있는 경우
     * @throws MessageException.NotEditable 삭제된 메시지이거나 텍스트 타입이 아닌 경우
     */
    fun editMessage(newContent: String) {
        // 24시간 제한 검증
        validateEditTimeLimit()

        // XSS 방지: HTML 특수문자 이스케이프
        val sanitizedContent = TextSanitizer.sanitize(newContent)

        // 내용 유효성 검사
        if (sanitizedContent.isBlank()) {
            throw MessageException.EmptyContent()
        }

        // 삭제된 메시지 확인
        if (this.content.isDeleted) {
            throw MessageException.NotEditable("삭제된 메시지는 수정할 수 없습니다.")
        }

        // 메시지 타입 확인 (TEXT 타입만 수정 가능)
        if (this.content.type != MessageType.TEXT) {
            throw MessageException.NotEditable("텍스트 타입의 메시지만 수정할 수 있습니다.")
        }

        // 내용 업데이트 및 편집 여부 설정
        this.content = this.content.copy(
            text = sanitizedContent,
            isEdited = true
        )
        this.updatedAt = Instant.now()
    }

    /**
     * 메시지 수정 시간 제한을 검증합니다.
     * 메시지는 생성 후 24시간 이내에만 수정 가능합니다.
     *
     * @throws MessageException.EditTimeExpired 24시간이 지난 경우
     */
    private fun validateEditTimeLimit() {
        val messageAge = Duration.between(
            this.createdAt ?: Instant.now(),
            Instant.now()
        )

        if (messageAge.toHours() >= 24) {
            throw MessageException.EditTimeExpired()
        }
    }

    /**
     * 메시지를 삭제 상태로 변경합니다.
     */
    fun markAsDeleted() {
        // 삭제 상태로 변경 (isDeleted 플래그 설정)
        this.content = this.content.copy(isDeleted = true)
        this.updatedAt = Instant.now()
    }

    /**
     * URL 미리보기 정보를 설정합니다.
     *
     * @param urlPreview URL 미리보기 정보
     */
    fun setUrlPreview(urlPreview: ChatMessageMetadata.UrlPreview) {
        this.metadata = this.metadata.copy(
            urlPreview = urlPreview,
            needsUrlPreview = false
        )
        this.updatedAt = Instant.now()
    }

    /**
     * URL 미리보기가 필요함을 표시합니다.
     *
     * @param url 미리보기가 필요한 URL
     */
    fun markNeedsUrlPreview(url: String) {
        this.metadata = this.metadata.copy(
            needsUrlPreview = true,
            previewUrl = url
        )
        this.updatedAt = Instant.now()
    }

    /**
     * 메시지에서 멘션된 사용자들을 추출하고 업데이트합니다.
     *
     * @param mentionExtractor 멘션 추출 함수
     */
    fun updateMentions(mentionExtractor: (String) -> Set<UserId>) {
        if (this.content.type == MessageType.TEXT) {
            this.mentions = mentionExtractor(this.content.text)
            this.updatedAt = Instant.now()
        }
    }

    /**
     * 첨부파일 크기를 검증합니다.
     *
     * @param maxAttachmentSize 최대 첨부파일 크기 (바이트)
     * @throws MessageException.AttachmentTooLarge 첨부파일 크기가 제한을 초과하는 경우
     */
    fun validateAttachmentSizes(maxAttachmentSize: Long) {
        content.attachments.forEach { attachment ->
            if (attachment.size > maxAttachmentSize) {
                val sizeMB = attachment.size / (1024 * 1024)
                val maxSizeMB = maxAttachmentSize / (1024 * 1024)
                throw MessageException.AttachmentTooLarge(
                    "첨부파일 '${attachment.filename}'의 크기가 너무 큽니다. " +
                    "(파일 크기: ${sizeMB}MB, 최대: ${maxSizeMB}MB)"
                )
            }
        }
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
            roomId: ChatRoomId,
            senderId: UserId,
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
                status = MessageStatus.SENT,  // SENDING → SENT로 변경
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
            getCachedPreview: (String) -> ChatMessageMetadata.UrlPreview?
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

            if (cachedPreview != null) {
                // 캐시된 미리보기가 있으면 설정
                message.setUrlPreview(cachedPreview)
            } else {
                // 캐시된 미리보기가 없으면 필요함을 표시
                message.markNeedsUrlPreview(url)
            }
            
            return message
        }

    }

}
