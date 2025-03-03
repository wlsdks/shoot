package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.AnnouncementRequest
import com.stark.shoot.adapter.`in`.web.dto.chatroom.InvitationRequest
import com.stark.shoot.adapter.`in`.web.dto.chatroom.ManageParticipantRequest
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
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
    ): ResponseEntity<Any> {
        // 새로운 UseCase 혹은 기존 createChatRoomUseCase를 재활용
        // 내부에서 "이미 존재하는 1:1 채팅방" 체크 후,
        // 없으면 새로 만들고 그 채팅방 정보 반환
        val room = createChatRoomUseCase.createDirectChat(
            userId.toObjectId(),
            friendId.toObjectId()
        )
        return ResponseEntity.ok(room)
    }

    @Operation(summary = "채팅방 참여자 추가", description = "채팅방에 참여자를 추가합니다.")
    @PostMapping("/{roomId}/participants")
    fun addParticipant(
        @PathVariable roomId: String,
        @RequestBody request: ManageParticipantRequest
    ): ResponseEntity<Boolean> {
        val result = manageChatRoomUseCase.addParticipant(roomId, request.userId.toObjectId())
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "채팅방 참여자 제거", description = "채팅방에서 참여자를 제거합니다.")
    @DeleteMapping("/{roomId}/participants")
    fun removeParticipant(
        @PathVariable roomId: String,
        @RequestBody request: ManageParticipantRequest
    ): ResponseEntity<Boolean> {
        val result = manageChatRoomUseCase.removeParticipant(roomId, request.userId.toObjectId())
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "채팅방 퇴장", description = "현재 사용자가 채팅방에서 퇴장합니다.")
    @DeleteMapping("/{roomId}/exit")
    fun exitChatRoom(
        @PathVariable roomId: String,
        @RequestParam userId: String
    ): ResponseEntity<Boolean> {
        val result = manageChatRoomUseCase.removeParticipant(roomId, userId.toObjectId())
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "채팅방 초대", description = "채팅방에 사용자를 초대합니다.")
    @PostMapping("/{roomId}/invite")
    fun inviteParticipant(
        @PathVariable roomId: String,
        @RequestBody request: InvitationRequest
    ): ResponseEntity<Boolean> {
        val result = manageChatRoomUseCase.addParticipant(roomId, request.userId.toObjectId())
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "채팅방 공지사항 설정", description = "채팅방의 공지사항을 업데이트합니다.")
    @PutMapping("/{roomId}/announcement")
    fun updateAnnouncement(
        @PathVariable roomId: String,
        @RequestBody request: AnnouncementRequest
    ): ResponseEntity<Unit> {
        manageChatRoomUseCase.updateAnnouncement(roomId, request.announcement)
        return ResponseEntity.noContent().build()
    }

}
