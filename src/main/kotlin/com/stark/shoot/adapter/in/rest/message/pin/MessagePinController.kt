package com.stark.shoot.adapter.`in`.rest.message.pin

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.pin.PinResponse
import com.stark.shoot.application.port.`in`.message.pin.MessagePinUseCase
import com.stark.shoot.application.port.`in`.message.pin.command.PinMessageCommand
import com.stark.shoot.application.port.`in`.message.pin.command.UnpinMessageCommand
import com.stark.shoot.application.port.out.message.pin.MessagePinQueryPort
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "메시지 고정", description = "메시지 고정 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessagePinController(
    private val messagePinUseCase: MessagePinUseCase,
    private val messagePinQueryPort: MessagePinQueryPort
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
        val command = PinMessageCommand.of(messageId, authentication)
        val updatedMessage = messagePinUseCase.pinMessage(command)

        // MessagePin Aggregate 조회
        val messagePin = messagePinQueryPort.findByMessageId(updatedMessage.id!!)

        return ResponseDto.success(PinResponse.from(updatedMessage, messagePin), "메시지가 고정되었습니다.")
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
        val command = UnpinMessageCommand.of(messageId, authentication)
        val updatedMessage = messagePinUseCase.unpinMessage(command)

        // 고정 해제 후이므로 messagePin은 null
        return ResponseDto.success(PinResponse.from(updatedMessage, null), "메시지 고정이 해제되었습니다.")
    }

}
