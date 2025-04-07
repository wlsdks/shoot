package com.stark.shoot.adapter.`in`.web.message.pin

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.pin.PinResponse
import com.stark.shoot.adapter.`in`.web.dto.message.pin.PinnedMessagesResponse
import com.stark.shoot.application.port.`in`.message.pin.MessagePinUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "메시지 고정", description = "메시지 고정 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessagePinController(
    private val messagePinUseCase: MessagePinUseCase
) {

    @Operation(
        summary = "메시지 고정",
        description = "중요한 메시지를 채팅방에 고정합니다. (이미 존재하면 해제하고 새로 고정)"
    )
    @PostMapping("/{messageId}/pin")
    fun pinMessage(
        @PathVariable messageId: String,
        authentication: Authentication
    ): ResponseDto<PinResponse> {
        val userId = authentication.name.toLong()
        val updatedMessage = messagePinUseCase.pinMessage(messageId, userId)
        return ResponseDto.success(PinResponse.from(updatedMessage), "메시지가 고정되었습니다.")
    }

    @Operation(
        summary = "메시지 고정 해제",
        description = "고정된 메시지를 해제합니다."
    )
    @DeleteMapping("/{messageId}/pin")
    fun unpinMessage(
        @PathVariable messageId: String,
        authentication: Authentication
    ): ResponseDto<PinResponse> {
        val userId = authentication.name.toLong()
        val updatedMessage = messagePinUseCase.unpinMessage(messageId, userId)
        return ResponseDto.success(PinResponse.from(updatedMessage), "메시지 고정이 해제되었습니다.")
    }

    @Operation(
        summary = "고정된 메시지 목록 조회",
        description = "채팅방에 고정된 모든 메시지를 조회합니다."
    )
    @GetMapping("/pins")
    fun getPinnedMessages(
        @RequestParam roomId: Long
    ): ResponseDto<PinnedMessagesResponse> {
        val pinnedMessages = messagePinUseCase.getPinnedMessages(roomId)
        return ResponseDto.success(PinnedMessagesResponse.from(roomId, pinnedMessages))
    }

}