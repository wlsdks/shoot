package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.chatroom.AnnouncementRequest
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "채팅방", description = "채팅방 관련 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomNoticeController(
    private val manageChatRoomUseCase: ManageChatRoomUseCase
) {

    @Operation(summary = "채팅방 공지사항 설정", description = "채팅방의 공지사항을 업데이트합니다.")
    @PutMapping("/{roomId}/announcement")
    fun updateAnnouncement(
        @PathVariable roomId: Long,
        @RequestBody request: AnnouncementRequest
    ): ResponseDto<Unit> {
        manageChatRoomUseCase.updateAnnouncement(roomId, request.announcement)
        return ResponseDto.success(Unit, "공지사항이 업데이트되었습니다.")
    }

}