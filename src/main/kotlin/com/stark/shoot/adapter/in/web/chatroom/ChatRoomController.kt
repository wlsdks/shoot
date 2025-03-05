package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.chatroom.AnnouncementRequest
import com.stark.shoot.adapter.`in`.web.dto.chatroom.InvitationRequest
import com.stark.shoot.adapter.`in`.web.dto.chatroom.ManageParticipantRequest
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "채팅방", description = "채팅방 관련 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomController(
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val manageChatRoomUseCase: ManageChatRoomUseCase
) {

    @Operation(summary = "1:1 채팅방 생성", description = "특정 사용자와 친구의 1:1 채팅방을 생성합니다.")
    @PostMapping("/create/direct")
    fun createDirectChat(
        @RequestParam userId: String,
        @RequestParam friendId: String
    ): ResponseDto<ChatRoom> {
        return try {
            val room = createChatRoomUseCase.createDirectChat(
                userId.toObjectId(),
                friendId.toObjectId()
            )
            ResponseDto.success(room, "채팅방이 생성되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "채팅방 생성에 실패했습니다: ${e.message}",
                ApiException.ROOM_NOT_FOUND,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

    @Operation(summary = "채팅방 참여자 추가", description = "채팅방에 참여자를 추가합니다.")
    @PostMapping("/{roomId}/participants")
    fun addParticipant(
        @PathVariable roomId: String,
        @RequestBody request: ManageParticipantRequest
    ): ResponseDto<Boolean> {
        return try {
            val result = manageChatRoomUseCase.addParticipant(roomId, request.userId.toObjectId())
            ResponseDto.success(result, "참여자가 추가되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "참여자 추가에 실패했습니다: ${e.message}",
                ApiException.INVALID_INPUT,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

    @Operation(summary = "채팅방 참여자 제거", description = "채팅방에서 참여자를 제거합니다.")
    @DeleteMapping("/{roomId}/participants")
    fun removeParticipant(
        @PathVariable roomId: String,
        @RequestBody request: ManageParticipantRequest
    ): ResponseDto<Boolean> {
        return try {
            val result = manageChatRoomUseCase.removeParticipant(roomId, request.userId.toObjectId())
            ResponseDto.success(result, "참여자가 제거되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "참여자 제거에 실패했습니다: ${e.message}",
                ApiException.INVALID_INPUT,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

    @Operation(summary = "채팅방 퇴장", description = "현재 사용자가 채팅방에서 퇴장합니다.")
    @DeleteMapping("/{roomId}/exit")
    fun exitChatRoom(
        @PathVariable roomId: String,
        @RequestParam userId: String
    ): ResponseDto<Boolean> {
        return try {
            val result = manageChatRoomUseCase.removeParticipant(roomId, userId.toObjectId())
            ResponseDto.success(result, "채팅방에서 퇴장했습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "채팅방 퇴장에 실패했습니다: ${e.message}",
                ApiException.USER_NOT_IN_ROOM,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

    @Operation(summary = "채팅방 초대", description = "채팅방에 사용자를 초대합니다.")
    @PostMapping("/{roomId}/invite")
    fun inviteParticipant(
        @PathVariable roomId: String,
        @RequestBody request: InvitationRequest
    ): ResponseDto<Boolean> {
        return try {
            val result = manageChatRoomUseCase.addParticipant(roomId, request.userId.toObjectId())
            ResponseDto.success(result, "사용자를 채팅방에 초대했습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "초대에 실패했습니다: ${e.message}",
                ApiException.INVALID_INPUT,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

    @Operation(summary = "채팅방 공지사항 설정", description = "채팅방의 공지사항을 업데이트합니다.")
    @PutMapping("/{roomId}/announcement")
    fun updateAnnouncement(
        @PathVariable roomId: String,
        @RequestBody request: AnnouncementRequest
    ): ResponseDto<Unit> {
        return try {
            manageChatRoomUseCase.updateAnnouncement(roomId, request.announcement)
            ResponseDto.success(Unit, "공지사항이 업데이트되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "공지사항 업데이트에 실패했습니다: ${e.message}",
                ApiException.INVALID_INPUT,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

}