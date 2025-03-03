package com.stark.shoot.adapter.`in`.web.message

import com.stark.shoot.adapter.`in`.web.socket.dto.ChatMessageResponse
import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
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
    private val forwardMessageUseCase: ForwardMessageUseCase
) {

    @Operation(summary = "메시지 전달", description = "원본 메시지를 다른 채팅방이나 사용자에게 전달합니다.")
    @PostMapping("/forward")
    fun forwardMessage(
        @RequestParam originalMessageId: String,
        @RequestParam targetRoomId: String,
        @RequestParam forwardingUserId: String // 보통 토큰에서 사용자 ID를 추출하여 전달합니다.
    ): ChatMessageResponse {
        // 전달된 메시지를 생성하고 저장합니다.
        val forwardedMessage = forwardMessageUseCase.forwardMessage(
            originalMessageId = originalMessageId,
            targetRoomId = targetRoomId,
            forwardingUserId = forwardingUserId
        )

        // 전달된 메시지의 상태와 내용을 반환합니다.
        return ChatMessageResponse(
            status = forwardedMessage.status.name,
            content = forwardedMessage.content.text
        )
    }

}