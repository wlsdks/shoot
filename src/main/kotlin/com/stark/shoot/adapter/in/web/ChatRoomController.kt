package com.stark.shoot.adapter.`in`.web

import com.stark.shoot.adapter.`in`.web.dto.CreateChatRoomRequest
import com.stark.shoot.adapter.`in`.web.dto.ManageParticipantRequest
import com.stark.shoot.adapter.`in`.web.dto.UpdateRoomSettingsRequest
import com.stark.shoot.application.port.`in`.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.ManageChatRoomUseCase
import com.stark.shoot.domain.chat.room.ChatRoom
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomController(
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val manageChatRoomUseCase: ManageChatRoomUseCase
) {

    /**
     * 채팅방 생성 API
     * @param request 채팅방 생성 요청 데이터
     * @return 생성된 채팅방
     */
    @PostMapping
    fun createChatRoom(@RequestBody request: CreateChatRoomRequest): ResponseEntity<ChatRoom> {
        val chatRoom = createChatRoomUseCase.create(
            title = request.title,
            participants = request.participants
        )
        return ResponseEntity.ok(chatRoom)
    }

    /**
     * 채팅방에 참여자 추가
     * @param roomId 채팅방 ID
     * @param request 참여자 추가 요청 데이터
     * @return 성공 여부
     */
    @PostMapping("/{roomId}/participants")
    fun addParticipant(
        @PathVariable roomId: String,
        @RequestBody request: ManageParticipantRequest
    ): ResponseEntity<Boolean> {
        val result = manageChatRoomUseCase.addParticipant(roomId, request.userId)
        return ResponseEntity.ok(result)
    }

    /**
     * 채팅방에서 참여자 제거
     * @param roomId 채팅방 ID
     * @param request 참여자 제거 요청 데이터
     * @return 성공 여부
     */
    @DeleteMapping("/{roomId}/participants")
    fun removeParticipant(
        @PathVariable roomId: String,
        @RequestBody request: ManageParticipantRequest
    ): ResponseEntity<Boolean> {
        val result = manageChatRoomUseCase.removeParticipant(roomId, request.userId)
        return ResponseEntity.ok(result)
    }

    /**
     * 채팅방 설정 변경
     * @param roomId 채팅방 ID
     * @param request 설정 변경 요청 데이터
     */
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
