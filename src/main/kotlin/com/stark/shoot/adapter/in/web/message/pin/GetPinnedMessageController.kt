package com.stark.shoot.adapter.`in`.web.message.pin

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.pin.PinnedMessagesResponse
import com.stark.shoot.application.port.`in`.message.pin.GetPinnedMessageUseCase
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
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
    private val getPinnedMessageUseCase: GetPinnedMessageUseCase
) {

    @Operation(
        summary = "고정된 메시지 목록 조회",
        description = "채팅방에 고정된 모든 메시지를 조회합니다."
    )
    @GetMapping("/pins")
    fun getPinnedMessages(
        @RequestParam roomId: Long
    ): ResponseDto<PinnedMessagesResponse> {
        val pinnedMessages = getPinnedMessageUseCase.getPinnedMessages(ChatRoomId.from(roomId))
        return ResponseDto.success(PinnedMessagesResponse.from(roomId, pinnedMessages))
    }

}