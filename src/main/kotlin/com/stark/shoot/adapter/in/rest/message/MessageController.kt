package com.stark.shoot.adapter.`in`.rest.message

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.DeleteMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.EditMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.DeleteMessageUseCase
import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.DeleteMessageCommand
import com.stark.shoot.application.port.`in`.message.command.EditMessageCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "메시지", description = "메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessageController(
    private val editMessageUseCase: EditMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val chatMessageMapper: ChatMessageMapper
) {

    @Operation(
        summary = "메시지 편집",
        description = "메시지 내용을 수정합니다."
    )
    @PutMapping("/edit")
    fun editMessage(
        @RequestBody request: EditMessageRequest
    ): ResponseDto<MessageResponseDto> {
        val command = EditMessageCommand.of(request.messageId, request.newContent)
        val updatedMessage = editMessageUseCase.editMessage(command)

        return ResponseDto.success(chatMessageMapper.toDto(updatedMessage), "메시지가 수정되었습니다.")
    }

    @Operation(
        summary = "메시지 삭제",
        description = "메시지를 삭제 처리합니다."
    )
    @DeleteMapping("/delete")
    fun deleteMessage(
        @RequestBody request: DeleteMessageRequest
    ): ResponseDto<MessageResponseDto> {
        val command = DeleteMessageCommand.of(request.messageId)
        val deletedMessage = deleteMessageUseCase.deleteMessage(command)
        return ResponseDto.success(chatMessageMapper.toDto(deletedMessage), "메시지가 삭제되었습니다.")
    }

}
