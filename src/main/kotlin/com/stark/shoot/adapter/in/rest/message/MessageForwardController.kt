package com.stark.shoot.adapter.`in`.rest.message

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.socket.dto.ChatMessageResponse
import com.stark.shoot.application.port.`in`.message.ForwardMessageToUserUseCase
import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.ForwardMessageCommand
import com.stark.shoot.application.port.`in`.message.command.ForwardMessageToUserCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "메시지 전달", description = "메시지 전달(Forward) API")
@RestController
@RequestMapping("/api/v1/messages")
class MessageForwardController(
    private val forwardMessageUseCase: ForwardMessageUseCase,
    private val forwardMessageToUserUseCase: ForwardMessageToUserUseCase
) {

    @Operation(summary = "메시지 전달 (채팅방)", description = "원본 메시지를 다른 채팅방에 전달합니다.")
    @PostMapping("/forward")
    fun forwardMessage(
        @RequestParam originalMessageId: String,
        @RequestParam targetRoomId: Long,
        @RequestParam forwardingUserId: Long
    ): ResponseDto<ChatMessageResponse> {
        val command = ForwardMessageCommand.of(originalMessageId, targetRoomId, forwardingUserId)
        val forwardedMessage = forwardMessageUseCase.forwardMessage(command)

        val response = ChatMessageResponse(
            status = forwardedMessage.status.name,
            content = forwardedMessage.content.text
        )

        return ResponseDto.success(response, "메시지가 전달되었습니다.")
    }

    @Operation(summary = "메시지 전달 (사용자)", description = "원본 메시지를 특정 사용자(친구)에게 전달합니다.")
    @PostMapping("/forward/user")
    fun forwardMessageToUser(
        @RequestParam originalMessageId: String,
        @RequestParam targetUserId: Long,
        @RequestParam forwardingUserId: Long
    ): ResponseDto<ChatMessageResponse> {
        // 사용자에게 메시지 전달 (비즈니스 로직은 서비스에서 처리)
        val command = ForwardMessageToUserCommand.of(originalMessageId, targetUserId, forwardingUserId)
        val forwardedMessage = forwardMessageToUserUseCase.forwardMessageToUser(command)

        val response = ChatMessageResponse(
            status = forwardedMessage.status.name,
            content = forwardedMessage.content.text
        )

        return ResponseDto.success(response, "메시지가 사용자에게 전달되었습니다.")
    }

}
