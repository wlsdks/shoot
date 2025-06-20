package com.stark.shoot.adapter.`in`.web.socket.message.thread

import com.stark.shoot.adapter.`in`.web.socket.dto.ThreadDetailRequestDto
import com.stark.shoot.application.port.`in`.message.thread.GetThreadDetailUseCase
import com.stark.shoot.domain.common.vo.MessageId
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class GetThreadDetailStompHandler(
    private val getThreadDetailUseCase: GetThreadDetailUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @Operation(
        summary = "스레드 상세 조회 (WebSocket)",
        description = "스레드를 처음 열 때 사용하며 루트 메시지와 현재까지의 답글을 함께 반환합니다."
    )
    @MessageMapping("/thread/detail")
    fun handleGetThreadDetail(request: ThreadDetailRequestDto) {
        val detail = getThreadDetailUseCase.getThreadDetail(
            MessageId.from(request.threadId),
            MessageId.from(request.lastMessageId ?: ""),
            request.limit
        )

        messagingTemplate.convertAndSendToUser(request.userId.toString(), "/queue/thread/detail", detail)
    }

}
