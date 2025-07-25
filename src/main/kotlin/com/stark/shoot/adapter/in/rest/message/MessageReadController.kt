package com.stark.shoot.adapter.`in`.rest.message

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageResponseDto
import com.stark.shoot.application.port.`in`.message.GetMessagesUseCase
import com.stark.shoot.application.port.`in`.message.command.GetMessagesCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "메시지", description = "메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessageReadController(
    private val getMessagesUseCase: GetMessagesUseCase
) {

    @Operation(
        summary = "메시지 조회 (커서 기반 페이지네이션)",
        description = "특정 채팅방(roomId)의 메시지를 `_id` 기준으로 페이지네이션하여 조회합니다."
    )
    @GetMapping("/get")
    fun getMessages(
        @RequestParam roomId: Long,
        @RequestParam(required = false) lastMessageId: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseDto<List<MessageResponseDto>> {
        val command = GetMessagesCommand.of(roomId, lastMessageId, limit)
        val messageDtos = getMessagesUseCase.getMessages(command)

        return ResponseDto.success(messageDtos)
    }

}
