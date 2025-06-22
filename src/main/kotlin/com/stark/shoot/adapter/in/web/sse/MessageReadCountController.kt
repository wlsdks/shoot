package com.stark.shoot.adapter.`in`.web.sse

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.application.port.`in`.message.mark.MessageReadUseCase
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "읽음 상태", description = "메시지 읽음 상태 관련 API")
@RequestMapping("/api/v1/read-status")
@RestController
class MessageReadCountController(
    private val messageReadUseCase: MessageReadUseCase
) {

    @Operation(
        summary = "전체 메시지 읽음 처리",
        description = """
            - 채팅방의 모든 메시지를 읽음 처리합니다. '숫자 1 제거'
            - SSE를 통해 실시간으로 읽음 상태를 업데이트합니다.
        """
    )
    @PostMapping("/rooms/{roomId}/read-all")
    fun markAllMessagesAsRead(
        @PathVariable roomId: Long,
        @RequestParam userId: Long,
        @RequestParam(required = false) requestId: String?
    ): ResponseDto<Unit> {
        messageReadUseCase.markAllMessagesAsRead(
            ChatRoomId.from(roomId),
            UserId.from(userId),
            requestId
        )

        return ResponseDto.success(Unit, "모든 메시지가 읽음으로 처리되었습니다.")
    }

    @Operation(
        summary = "단일 메시지 읽음 처리",
        description = """
            - 특정 메시지를 읽음 처리합니다.
            - SSE를 통해 실시간으로 읽음 상태를 업데이트합니다.
        """
    )
    @PostMapping("/messages/{messageId}/read")
    fun markMessageAsRead(
        @PathVariable messageId: String,
        @RequestParam userId: Long
    ): ResponseDto<Unit> {
        messageReadUseCase.markMessageAsRead(
            MessageId.from(messageId),
            UserId.from(userId)
        )

        return ResponseDto.success(Unit, "메시지가 읽음으로 처리되었습니다.")
    }

}
