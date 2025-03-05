package com.stark.shoot.adapter.`in`.web.message

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.socket.dto.ChatMessageResponse
import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "메시지 전달", description = "메시지 전달(Forward) API")
@RestController
@RequestMapping("/api/v1/messages")
class MessageForwardController(
    private val forwardMessageUseCase: ForwardMessageUseCase
) {

    @Operation(summary = "메시지 전달", description = "원본 메시지를 다른 채팅방이나 사용자에게 전달합니다.")
    @PostMapping("/forward")
    fun forwardMessage(
        @RequestParam originalMessageId: String,
        @RequestParam targetRoomId: String,
        @RequestParam forwardingUserId: String
    ): ResponseDto<ChatMessageResponse> {
        return try {
            val forwardedMessage = forwardMessageUseCase.forwardMessage(
                originalMessageId = originalMessageId,
                targetRoomId = targetRoomId,
                forwardingUserId = forwardingUserId
            )

            val response = ChatMessageResponse(
                status = forwardedMessage.status.name,
                content = forwardedMessage.content.text
            )

            ResponseDto.success(response, "메시지가 전달되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "메시지 전달에 실패했습니다: ${e.message}",
                ApiException.RESOURCE_NOT_FOUND,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

}