package com.stark.shoot.adapter.`in`.rest.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.InvitationRequest
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ManageParticipantRequest
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.AddParticipantCommand
import com.stark.shoot.application.port.`in`.chatroom.command.RemoveParticipantCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "채팅방", description = "채팅방 관련 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class MultipleChatRoomController(
    private val manageChatRoomUseCase: ManageChatRoomUseCase
) {

    @Operation(summary = "채팅방 참여자 추가", description = "채팅방에 참여자를 추가합니다.")
    @PostMapping("/{roomId}/participants")
    fun addParticipant(
        @PathVariable roomId: Long,
        @RequestBody request: ManageParticipantRequest
    ): ResponseDto<Boolean> {
        val command = AddParticipantCommand.of(roomId, request.userId)
        val result = manageChatRoomUseCase.addParticipant(command)

        return ResponseDto.success(result, "참여자가 추가되었습니다.")
    }

    @Operation(summary = "채팅방 참여자 제거", description = "채팅방에서 참여자를 제거합니다.")
    @DeleteMapping("/{roomId}/participants")
    fun removeParticipant(
        @PathVariable roomId: Long,
        @RequestBody request: ManageParticipantRequest
    ): ResponseDto<Boolean> {
        val command = RemoveParticipantCommand.of(roomId, request.userId)
        val result = manageChatRoomUseCase.removeParticipant(command)

        return ResponseDto.success(result, "참여자가 제거되었습니다.")
    }

    @Operation(summary = "채팅방 초대", description = "채팅방에 사용자를 초대합니다.")
    @PostMapping("/{roomId}/invite")
    fun inviteParticipant(
        @PathVariable roomId: Long,
        @RequestBody request: InvitationRequest
    ): ResponseDto<Boolean> {
        val command = AddParticipantCommand.of(roomId, request.userId)
        val result = manageChatRoomUseCase.addParticipant(command)

        return ResponseDto.success(result, "사용자를 채팅방에 초대했습니다.")
    }
}
