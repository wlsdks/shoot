package com.stark.shoot.adapter.`in`.web.socket.mark

import com.stark.shoot.adapter.`in`.web.socket.dto.ChatReadRequest
import com.stark.shoot.application.port.`in`.message.mark.MarkMessageReadUseCase
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class MessageMarkStompHandler(
    private val markMessageReadUseCase: MarkMessageReadUseCase
) {

    @Operation(
        summary = "유저가 메시지를 읽었으면 숫자1을 사라지게 합니다.",
        description = """
            - 유저의 메시지에 있는 readBy를 true로 업데이트합니다.
            - 사용자의 화면에서는 메시지 옆에 표시되어 있던 1이 없어지게 됩니다.
        """
    )
    @MessageMapping("/read")
    fun handleRead(request: ChatReadRequest) {
        markMessageReadUseCase.markMessageAsRead(
            request.messageId,
            request.userId
        )
    }

}