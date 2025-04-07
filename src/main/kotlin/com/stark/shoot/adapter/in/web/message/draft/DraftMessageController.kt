package com.stark.shoot.adapter.`in`.web.message.draft

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.draft.DraftMessageResponseDto
import com.stark.shoot.application.port.`in`.message.draft.DraftMessageUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "메시지 임시저장", description = "메시지 임시저장 관련 API")
@RestController
@RequestMapping("/api/v1/messages/drafts")
class DraftMessageController(
    private val draftMessageUseCase: DraftMessageUseCase
) {

    @Operation(summary = "임시 메시지 저장", description = "작성 중인 메시지를 임시 저장합니다.")
    @PostMapping
    fun saveDraft(
        @RequestParam roomId: Long,
        @RequestParam content: String,
        @RequestParam(required = false, defaultValue = "[]") attachments: List<String>,
        @RequestParam(required = false, defaultValue = "[]") mentions: Set<String>,
        authentication: Authentication
    ): ResponseDto<DraftMessageResponseDto> {
        val userId = authentication.name.toLong()

        val result = draftMessageUseCase.saveDraft(
            userId = userId,
            roomId = roomId,
            content = content,
            attachments = attachments,
            mentions = mentions
        )

        return ResponseDto.success(result, "메시지가 임시 저장되었습니다.")
    }

    @Operation(summary = "임시 메시지 수정", description = "임시 저장된 메시지를 수정합니다.")
    @PutMapping("/{draftId}")
    fun updateDraft(
        @PathVariable draftId: String,
        @RequestParam content: String,
        @RequestParam(required = false, defaultValue = "[]") attachments: List<String>,
        @RequestParam(required = false, defaultValue = "[]") mentions: Set<String>,
        authentication: Authentication
    ): ResponseDto<DraftMessageResponseDto> {
        val userId = authentication.name.toLong()

        val result = draftMessageUseCase.updateDraft(
            draftId = draftId,
            userId = userId,
            content = content,
            attachments = attachments,
            mentions = mentions
        )

        return ResponseDto.success(result, "임시 메시지가 수정되었습니다.")
    }

    @Operation(summary = "임시 메시지 삭제", description = "임시 저장된 메시지를 삭제합니다.")
    @DeleteMapping("/{draftId}")
    fun deleteDraft(
        @PathVariable draftId: String,
        authentication: Authentication
    ): ResponseDto<Boolean> {
        val userId = authentication.name.toLong()
        val result = draftMessageUseCase.deleteDraft(draftId, userId)
        return ResponseDto.success(result, "임시 메시지가 삭제되었습니다.")
    }

    @Operation(summary = "채팅방별 임시 메시지 조회", description = "특정 채팅방의 임시 저장된 메시지를 조회합니다.")
    @GetMapping("/room/{roomId}")
    fun getDraftByRoom(
        @PathVariable roomId: Long,
        authentication: Authentication
    ): ResponseDto<DraftMessageResponseDto?> {
        val userId = authentication.name.toLong()
        val result = draftMessageUseCase.getDraftByRoom(userId, roomId)
        return ResponseDto.success(result)
    }

    @Operation(summary = "모든 임시 메시지 조회", description = "사용자의 모든 채팅방 임시 저장 메시지를 조회합니다.")
    @GetMapping
    fun getAllDrafts(
        authentication: Authentication
    ): ResponseDto<Map<Long, DraftMessageResponseDto>> {
        val userId = authentication.name.toLong()
        val result = draftMessageUseCase.getAllDrafts(userId)
        return ResponseDto.success(result)
    }

}