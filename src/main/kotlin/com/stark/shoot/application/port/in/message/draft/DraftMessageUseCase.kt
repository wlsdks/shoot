package com.stark.shoot.application.port.`in`.message.draft

import com.stark.shoot.adapter.`in`.web.dto.message.draft.DraftMessageResponseDto

interface DraftMessageUseCase {
    fun saveDraft(
        userId: String,
        roomId: String,
        content: String,
        attachments: List<String> = emptyList(),
        mentions: Set<String> = emptySet()
    ): DraftMessageResponseDto

    fun updateDraft(
        draftId: String,
        userId: String,
        content: String,
        attachments: List<String> = emptyList(),
        mentions: Set<String> = emptySet()
    ): DraftMessageResponseDto

    fun deleteDraft(
        draftId: String,
        userId: String
    ): Boolean

    fun getDraftByRoom(
        userId: String,
        roomId: String
    ): DraftMessageResponseDto?

    fun getAllDrafts(userId: String): Map<String, DraftMessageResponseDto>
}