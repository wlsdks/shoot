package com.stark.shoot.adapter.`in`.web.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionListResponse
import com.stark.shoot.application.port.`in`.message.reaction.GetMessageReactionUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "메시지 반응", description = "메시지 반응 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class GetMessageReactionController(
    private val getMessageReactionUseCase: GetMessageReactionUseCase
) {

    @Operation(
        summary = "메시지 반응 조회",
        description = "메시지에 추가된 모든 반응을 조회합니다."
    )
    @GetMapping("/{messageId}/reactions")
    fun getReactions(
        @PathVariable messageId: String
    ): ResponseDto<ReactionListResponse> {
        val reactions = getMessageReactionUseCase.getReactions(messageId)
        val response = ReactionListResponse.from(messageId, reactions)
        return ResponseDto.success(response)
    }


    @Operation(
        summary = "지원하는 반응 타입 조회",
        description = "시스템에서 지원하는 모든 반응 타입을 조회합니다."
    )
    @GetMapping("/reactions/types")
    fun getReactionTypes(): ResponseDto<List<Map<String, String>>> {
        val reactionTypes = getMessageReactionUseCase.getSupportedReactionTypes()
            .map { type ->
                mapOf(
                    "code" to type.code,
                    "emoji" to type.emoji,
                    "description" to type.description
                )
            }
        return ResponseDto.success(reactionTypes)
    }

}
