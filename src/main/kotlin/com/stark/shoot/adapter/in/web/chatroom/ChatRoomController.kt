package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.CreateChatRoomRequest
import com.stark.shoot.adapter.`in`.web.dto.ManageParticipantRequest
import com.stark.shoot.adapter.`in`.web.dto.UpdateRoomSettingsRequest
import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.RetrieveChatRoomUseCase
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.common.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "채팅방", description = "채팅방 관련 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomController(
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val retrieveChatRoomUseCase: RetrieveChatRoomUseCase,
    private val manageChatRoomUseCase: ManageChatRoomUseCase
) {

    @Operation(
        summary = "사용자의 채팅방 목록 조회",
        description = "특정 사용자의 채팅방 전체 목록을 조회합니다."
    )
    @GetMapping
    fun getChatRooms(
        @RequestParam userId: ObjectId
    ): ResponseEntity<List<ChatRoomResponse>> {
        val chatRooms = retrieveChatRoomUseCase.getChatRoomsForUser(userId)
        return ResponseEntity.ok(chatRooms)
    }

    @Operation(
        summary = "채팅방 생성",
        description = "채팅방을 생성합니다.",
    )
    @PostMapping("/create")
    fun createChatRoom(
        @RequestBody request: CreateChatRoomRequest
    ): ResponseEntity<ChatRoom> {
        // 문자열을 ObjectId로 변환 (유효하지 않은 값에 대해서는 예외 처리 필요)
        val participantIds = request.participants.map {
            it.toObjectId()
        }.toMutableSet()

        val chatRoom = createChatRoomUseCase.create(
            title = request.title,
            participants = participantIds
        )

        return ResponseEntity.ok(chatRoom)
    }


    @Operation(
        summary = "채팅방 참여자 추가",
        description = "채팅방에 참여자를 추가합니다."
    )
    @PostMapping("/{roomId}/participants")
    fun addParticipant(
        @PathVariable roomId: String,
        @RequestBody request: ManageParticipantRequest
    ): ResponseEntity<Boolean> {
        val result = manageChatRoomUseCase.addParticipant(roomId, request.userId.toObjectId())
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "채팅방 참여자 제거",
        description = "채팅방에서 참여자를 제거합니다."
    )
    @DeleteMapping("/{roomId}/participants")
    fun removeParticipant(
        @PathVariable roomId: String,
        @RequestBody request: ManageParticipantRequest
    ): ResponseEntity<Boolean> {
        val result = manageChatRoomUseCase.removeParticipant(roomId, request.userId.toObjectId())
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "채팅방 설정 변경",
        description = "채팅방의 설정을 변경합니다."
    )
    @PutMapping("/{roomId}/settings")
    fun updateRoomSettings(
        @PathVariable roomId: String,
        @RequestBody request: UpdateRoomSettingsRequest
    ): ResponseEntity<Unit> {
        manageChatRoomUseCase.updateRoomSettings(
            roomId = roomId,
            title = request.title,
            notificationEnabled = request.notificationEnabled
        )
        return ResponseEntity.noContent().build()
    }

}
