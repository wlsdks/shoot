package com.stark.shoot.application.port.`in`.message.draft

import com.stark.shoot.adapter.`in`.web.dto.message.draft.DraftMessageResponseDto

interface DraftMessageUseCase {
    fun saveDraft(
        userId: Long,
        roomId: Long,
        content: String,
        attachments: List<String> = emptyList(),
        mentions: Set<String> = emptySet()
    ): DraftMessageResponseDto

    fun updateDraft(
        draftId: String,
        userId: Long,
        content: String,
        attachments: List<String> = emptyList(),
        mentions: Set<String> = emptySet()
    ): DraftMessageResponseDto

    fun deleteDraft(draftId: String, userId: Long): Boolean
    fun getDraftByRoom(userId: Long, roomId: Long): DraftMessageResponseDto?
    fun getAllDrafts(userId: Long): Map<Long, DraftMessageResponseDto>
}