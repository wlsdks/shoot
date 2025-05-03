package com.stark.shoot.adapter.`in`.web.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionListResponse
import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionRequest
import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.application.port.`in`.message.reaction.MessageReactionUseCase
import com.stark.shoot.infrastructure.enumerate.ReactionType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "메시지 반응", description = "메시지 반응 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessageReactionController(
    private val messageReactionUseCase: MessageReactionUseCase
) {


    @Operation(
        summary = "메시지 반응 조회",
        description = "메시지에 추가된 모든 반응을 조회합니다."
    )
    @GetMapping("/{messageId}/reactions")
    fun getReactions(
        @PathVariable messageId: String
    ): ResponseDto<ReactionListResponse> {
        val reactions = messageReactionUseCase.getReactions(messageId)
        val response = ReactionListResponse.from(messageId, reactions)
        return ResponseDto.success(response)
    }


    @Operation(
        summary = "지원하는 반응 타입 조회",
        description = "시스템에서 지원하는 모든 반응 타입을 조회합니다."
    )
    @GetMapping("/reactions/types")
    fun getReactionTypes(): ResponseDto<List<Map<String, String>>> {
        val reactionTypes = messageReactionUseCase.getSupportedReactionTypes()
            .map { type ->
                mapOf(
                    "code" to type.code,
                    "emoji" to type.emoji,
                    "description" to type.description
                )
            }
        return ResponseDto.success(reactionTypes)
    }

    @Operation(
        summary = "메시지 반응 토글",
        description = "메시지에 반응을 토글합니다. 같은 반응을 선택하면 제거하고, 다른 반응을 선택하면 기존 반응을 제거하고 새 반응을 추가합니다."
    )
    @PutMapping("/{messageId}/reactions")
    fun toggleReaction(
        @PathVariable messageId: String,
        @RequestBody request: ReactionRequest,
        authentication: Authentication
    ): ResponseDto<ReactionResponse> {
        val userId = authentication.name.toLong()
        val response = messageReactionUseCase.toggleReaction(messageId, userId, request.reactionType)
        return ResponseDto.success(response, "반응이 토글되었습니다.")
    }
}
