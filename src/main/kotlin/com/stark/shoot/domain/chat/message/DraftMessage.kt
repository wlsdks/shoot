package com.stark.shoot.domain.chat.message

import java.time.Instant

// 채팅 메시지의 임시 저장 정보
data class DraftMessage(
    val id: String? = null,
    val userId: Long,                      // 사용자 ID
    val roomId: Long,                      // 채팅방 ID
    val content: String,                     // 내용
    val attachments: List<String> = emptyList(), // 첨부파일 ID 목록
    val mentions: Set<String> = emptySet(),  // 멘션된 사용자 ID 목록
    val createdAt: Instant = Instant.now(),  // 생성 시간
    val updatedAt: Instant? = null,          // 수정 시간
    val metadata: MutableMap<String, Any> = mutableMapOf() // 추가 메타데이터
) {
    /**
     * 임시 저장 메시지 내용 업데이트
     *
     * @param newContent 새 내용
     * @return 업데이트된 DraftMessage 객체
     */
    fun updateContent(newContent: String): DraftMessage {
        return this.copy(
            content = newContent,
            updatedAt = Instant.now()
        )
    }

    /**
     * 첨부파일 추가
     *
     * @param attachmentId 추가할 첨부파일 ID
     * @return 업데이트된 DraftMessage 객체
     */
    fun addAttachment(attachmentId: String): DraftMessage {
        val updatedAttachments = this.attachments.toMutableList()
        updatedAttachments.add(attachmentId)
        return this.copy(
            attachments = updatedAttachments,
            updatedAt = Instant.now()
        )
    }

    /**
     * 여러 첨부파일 추가
     *
     * @param attachmentIds 추가할 첨부파일 ID 목록
     * @return 업데이트된 DraftMessage 객체
     */
    fun addAttachments(attachmentIds: List<String>): DraftMessage {
        val updatedAttachments = this.attachments.toMutableList()
        updatedAttachments.addAll(attachmentIds)
        return this.copy(
            attachments = updatedAttachments,
            updatedAt = Instant.now()
        )
    }

    /**
     * 첨부파일 제거
     *
     * @param attachmentId 제거할 첨부파일 ID
     * @return 업데이트된 DraftMessage 객체
     */
    fun removeAttachment(attachmentId: String): DraftMessage {
        val updatedAttachments = this.attachments.filter { it != attachmentId }
        return this.copy(
            attachments = updatedAttachments,
            updatedAt = Instant.now()
        )
    }

    /**
     * 모든 첨부파일 제거
     *
     * @return 첨부파일이 없는 DraftMessage 객체
     */
    fun clearAttachments(): DraftMessage {
        return this.copy(
            attachments = emptyList(),
            updatedAt = Instant.now()
        )
    }

    /**
     * 멘션 추가
     *
     * @param userId 멘션할 사용자 ID
     * @return 업데이트된 DraftMessage 객체
     */
    fun addMention(userId: String): DraftMessage {
        val updatedMentions = this.mentions.toMutableSet()
        updatedMentions.add(userId)
        return this.copy(
            mentions = updatedMentions,
            updatedAt = Instant.now()
        )
    }

    /**
     * 여러 멘션 추가
     *
     * @param userIds 멘션할 사용자 ID 목록
     * @return 업데이트된 DraftMessage 객체
     */
    fun addMentions(userIds: Collection<String>): DraftMessage {
        val updatedMentions = this.mentions.toMutableSet()
        updatedMentions.addAll(userIds)
        return this.copy(
            mentions = updatedMentions,
            updatedAt = Instant.now()
        )
    }

    /**
     * 멘션 제거
     *
     * @param userId 제거할 멘션의 사용자 ID
     * @return 업데이트된 DraftMessage 객체
     */
    fun removeMention(userId: String): DraftMessage {
        val updatedMentions = this.mentions.filter { it != userId }.toSet()
        return this.copy(
            mentions = updatedMentions,
            updatedAt = Instant.now()
        )
    }

    /**
     * 모든 멘션 제거
     *
     * @return 멘션이 없는 DraftMessage 객체
     */
    fun clearMentions(): DraftMessage {
        return this.copy(
            mentions = emptySet(),
            updatedAt = Instant.now()
        )
    }

    /**
     * 메타데이터 항목 추가
     *
     * @param key 메타데이터 키
     * @param value 메타데이터 값
     * @return 업데이트된 DraftMessage 객체
     */
    fun addMetadata(key: String, value: Any): DraftMessage {
        val updatedMetadata = this.metadata.toMutableMap()
        updatedMetadata[key] = value
        return this.copy(
            metadata = updatedMetadata,
            updatedAt = Instant.now()
        )
    }

    /**
     * 메타데이터 항목 제거
     *
     * @param key 제거할 메타데이터 키
     * @return 업데이트된 DraftMessage 객체
     */
    fun removeMetadata(key: String): DraftMessage {
        val updatedMetadata = this.metadata.toMutableMap()
        updatedMetadata.remove(key)
        return this.copy(
            metadata = updatedMetadata,
            updatedAt = Instant.now()
        )
    }

    /**
     * 여러 메타데이터 항목 업데이트
     *
     * @param newMetadata 업데이트할 메타데이터 맵
     * @return 업데이트된 DraftMessage 객체
     */
    fun updateMetadata(newMetadata: Map<String, Any>): DraftMessage {
        val updatedMetadata = this.metadata.toMutableMap()
        updatedMetadata.putAll(newMetadata)
        return this.copy(
            metadata = updatedMetadata,
            updatedAt = Instant.now()
        )
    }

    /**
     * 모든 메타데이터 초기화
     *
     * @return 메타데이터가 비어있는 DraftMessage 객체
     */
    fun clearMetadata(): DraftMessage {
        return this.copy(
            metadata = mutableMapOf(),
            updatedAt = Instant.now()
        )
    }
}
