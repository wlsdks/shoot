package com.stark.shoot.adapter.`in`.web.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionListResponse
import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionRequest
import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.application.port.`in`.message.reaction.MessageReactionUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "메시지 반응", description = "메시지 반응 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessageReactionController(
    private val messageReactionUseCase: MessageReactionUseCase
) {

    @Operation(
        summary = "메시지에 반응 추가",
        description = "메시지에 이모지 등의 반응을 추가합니다."
    )
    @PostMapping("/{messageId}/reactions")
    fun addReaction(
        @PathVariable messageId: String,
        @RequestBody request: ReactionRequest,
        authentication: Authentication
    ): ResponseEntity<ReactionResponse> {
        val userId = authentication.name // JWT에서 추출된 userId

        val updatedMessage = messageReactionUseCase.addReaction(
            messageId, userId, request.reactionType
        )

        return ResponseEntity.ok(ReactionResponse.from(updatedMessage))
    }

    @Operation(
        summary = "메시지 반응 제거",
        description = "메시지에서 특정 반응을 제거합니다."
    )
    @DeleteMapping("/{messageId}/reactions/{reactionType}")
    fun removeReaction(
        @PathVariable messageId: String,
        @PathVariable reactionType: String,
        authentication: Authentication
    ): ResponseEntity<ReactionResponse> {
        val userId = authentication.name

        val updatedMessage = messageReactionUseCase.removeReaction(
            messageId, userId, reactionType
        )

        return ResponseEntity.ok(ReactionResponse.from(updatedMessage))
    }

    @Operation(
        summary = "메시지 반응 조회",
        description = "메시지에 추가된 모든 반응을 조회합니다."
    )
    @GetMapping("/{messageId}/reactions")
    fun getReactions(
        @PathVariable messageId: String
    ): ResponseEntity<ReactionListResponse> {
        val reactions = messageReactionUseCase.getReactions(messageId)
        return ResponseEntity.ok(ReactionListResponse.from(messageId, reactions))
    }

}