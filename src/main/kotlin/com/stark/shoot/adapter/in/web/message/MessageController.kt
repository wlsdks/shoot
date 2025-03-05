package com.stark.shoot.adapter.`in`.web.message

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.DeleteMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.EditMessageRequest
import com.stark.shoot.application.port.`in`.message.DeleteMessageUseCase
import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "메시지", description = "메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessageController(
    private val editMessageUseCase: EditMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase
) {

    @Operation(
        summary = "메시지 편집",
        description = "메시지 내용을 수정합니다."
    )
    @PutMapping("/edit")
    fun editMessage(
        @RequestBody request: EditMessageRequest
    ): ResponseDto<ChatMessage> {
        return try {
            val updatedMessage = editMessageUseCase.editMessage(request.messageId, request.newContent)
            ResponseDto.success(updatedMessage, "메시지가 수정되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "메시지 수정에 실패했습니다: ${e.message}",
                ApiException.MESSAGE_EDIT_TIMEOUT,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

    @Operation(
        summary = "메시지 삭제",
        description = "메시지를 삭제 처리합니다."
    )
    @DeleteMapping("/delete")
    fun deleteMessage(
        @RequestBody request: DeleteMessageRequest
    ): ResponseDto<ChatMessage> {
        return try {
            val deletedMessage = deleteMessageUseCase.deleteMessage(request.messageId)
            ResponseDto.success(deletedMessage, "메시지가 삭제되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "메시지 삭제에 실패했습니다: ${e.message}",
                ApiException.MESSAGE_ALREADY_DELETED,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

}