package com.stark.shoot.adapter.`in`.rest.message.pin

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.pin.PinnedMessagesResponse
import com.stark.shoot.application.port.`in`.message.pin.GetPinnedMessageUseCase
import com.stark.shoot.application.port.`in`.message.pin.command.GetPinnedMessagesCommand
import com.stark.shoot.application.port.out.message.pin.MessagePinQueryPort
import com.stark.shoot.domain.chat.vo.ChatRoomId as ChatChatRoomId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "메시지 고정", description = "메시지 고정 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class GetPinnedMessageController(
    private val getPinnedMessageUseCase: GetPinnedMessageUseCase,
    private val messagePinQueryPort: MessagePinQueryPort
) {

    @Operation(
        summary = "고정된 메시지 목록 조회",
        description = "채팅방에 고정된 모든 메시지를 조회합니다."
    )
    @GetMapping("/pins")
    fun getPinnedMessages(
        @RequestParam roomId: Long
    ): ResponseDto<PinnedMessagesResponse> {
        val command = GetPinnedMessagesCommand.of(roomId)
        val pinnedMessages = getPinnedMessageUseCase.getPinnedMessages(command)

        // MessagePin Aggregate 조회
        val chatRoomId = ChatChatRoomId.from(roomId)
        val messagePins = messagePinQueryPort.findAllByRoomId(chatRoomId)

        return ResponseDto.success(PinnedMessagesResponse.from(roomId, pinnedMessages, messagePins))
    }

}
