package com.stark.shoot.adapter.`in`.rest.message.reaction

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.reaction.ReactionRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.reaction.ReactionResponse
import com.stark.shoot.application.port.`in`.message.reaction.ToggleMessageReactionUseCase
import com.stark.shoot.application.port.`in`.message.reaction.command.ToggleMessageReactionCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "메시지 반응", description = "메시지 반응 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class ToggleMessageReactionController(
    private val toggleMessageReactionUseCase: ToggleMessageReactionUseCase
) {

    @Operation(
        summary = "메시지 반응 토글",
        description = """
            메시지에 반응을 토글합니다. (3가지 상태)
            - 최초 반응 선택: 반응 추가
            - 추가된 반응을 선택: 반응 제거
            - 선택된 상태에서 다른 반응 선택: 기존 반응을 제거하고 새 반응을 추가
        """
    )
    @PutMapping("/{messageId}/reactions")
    fun toggleReaction(
        @PathVariable messageId: String,
        @RequestBody request: ReactionRequest,
        authentication: Authentication
    ): ResponseDto<ReactionResponse> {
        val command = ToggleMessageReactionCommand.of(messageId, authentication, request.reactionType)
        val response = toggleMessageReactionUseCase.toggleReaction(command)

        return ResponseDto.success(response, "반응이 토글되었습니다.")
    }

}
