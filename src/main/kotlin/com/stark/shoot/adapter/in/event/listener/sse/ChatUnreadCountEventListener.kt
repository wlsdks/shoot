package com.stark.shoot.adapter.`in`.event.listener.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.SendUpdateCommand
import com.stark.shoot.domain.event.MessageUnreadCountUpdatedEvent
import io.swagger.v3.oas.annotations.Operation
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ChatUnreadCountEventListener(
    private val sseEmitterUseCase: SseEmitterUseCase
) {

    @Operation(
        summary = "MessageUnreadCountUpdatedEvent 이벤트 핸들러",
        description = "이벤트 발생시 채팅방의 읽지 않은 메시지 개수를 업데이트합니다."
    )
    @EventListener
    fun handle(event: MessageUnreadCountUpdatedEvent) {
        event.unreadCounts.forEach { (userId, count) ->
            val command = SendUpdateCommand.of(
                userId = userId,
                roomId = event.roomId,
                unreadCount = count,
                lastMessage = event.lastMessage
            )
            sseEmitterUseCase.sendUpdate(command)
        }
    }

}
