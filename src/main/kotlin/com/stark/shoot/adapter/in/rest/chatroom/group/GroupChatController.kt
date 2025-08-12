package com.stark.shoot.adapter.`in`.rest.chatroom.group

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.group.CreateGroupChatRequest
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.group.GroupChatResponse
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.group.ManageParticipantsRequest
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.group.UpdateGroupTitleRequest
import com.stark.shoot.application.port.`in`.chatroom.group.CreateGroupChatUseCase
import com.stark.shoot.application.port.`in`.chatroom.group.ManageGroupChatUseCase
import com.stark.shoot.application.port.`in`.chatroom.group.command.CreateGroupChatCommand
import com.stark.shoot.application.port.`in`.chatroom.group.command.LeaveGroupChatCommand
import com.stark.shoot.application.port.`in`.chatroom.group.command.ManageGroupParticipantsCommand
import com.stark.shoot.application.port.`in`.chatroom.group.command.UpdateGroupChatTitleCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/chatrooms/group")
class GroupChatController(
    private val createGroupChatUseCase: CreateGroupChatUseCase,
    private val manageGroupChatUseCase: ManageGroupChatUseCase
) {
    
    private val logger = KotlinLogging.logger {}

    /**
     * 그룹 채팅방 생성
     */
    @PostMapping
    fun createGroupChat(
        @Valid @RequestBody request: CreateGroupChatRequest,
        @RequestParam userId: Long
    ): ResponseEntity<ResponseDto<GroupChatResponse>> {
        logger.info { "Creating group chat: title=${request.title}, participants=${request.participants.size}" }

        val command = CreateGroupChatCommand.of(request, userId)
        val chatRoom = createGroupChatUseCase.createGroupChat(command)
        val response = GroupChatResponse.from(chatRoom)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDto.success(response, "그룹 채팅방이 생성되었습니다."))
    }

    /**
     * 그룹 채팅방 참여자 관리 (추가/제거)
     */
    @PutMapping("/{chatRoomId}/participants")
    fun manageParticipants(
        @PathVariable chatRoomId: Long,
        @Valid @RequestBody request: ManageParticipantsRequest,
        @RequestParam userId: Long
    ): ResponseEntity<ResponseDto<GroupChatResponse>> {
        logger.info { 
            "Managing group chat participants: roomId=$chatRoomId, " +
            "toAdd=${request.participantsToAdd.size}, toRemove=${request.participantsToRemove.size}" 
        }
        val command = ManageGroupParticipantsCommand(
            chatRoomId = chatRoomId,
            participantsToAdd = request.participantsToAdd,
            participantsToRemove = request.participantsToRemove,
            managedBy = userId
        )
        val chatRoom = manageGroupChatUseCase.manageParticipants(command)
        val response = GroupChatResponse.from(chatRoom)

        return ResponseEntity.ok(ResponseDto.success(response, "참여자가 변경되었습니다."))
    }

    /**
     * 그룹 채팅방 제목 변경
     */
    @PutMapping("/{chatRoomId}/title")
    fun updateTitle(
        @PathVariable chatRoomId: Long,
        @Valid @RequestBody request: UpdateGroupTitleRequest,
        @RequestParam userId: Long
    ): ResponseEntity<ResponseDto<GroupChatResponse>> {
        logger.info { "Updating group chat title: roomId=$chatRoomId, newTitle=${request.newTitle}" }
        val command = UpdateGroupChatTitleCommand(
            chatRoomId = chatRoomId,
            newTitle = request.newTitle,
            updatedBy = userId
        )
        val chatRoom = manageGroupChatUseCase.updateTitle(command)
        val response = GroupChatResponse.from(chatRoom)

        return ResponseEntity.ok(ResponseDto.success(response, "채팅방 제목이 변경되었습니다."))
    }

    /**
     * 그룹 채팅방 나가기
     */
    @DeleteMapping("/{chatRoomId}/participants/me")
    fun leaveGroup(
        @PathVariable chatRoomId: Long,
        @RequestParam userId: Long
    ): ResponseEntity<ResponseDto<GroupChatResponse?>> {
        logger.info { "User leaving group chat: roomId=$chatRoomId, userId=$userId" }
        val command = LeaveGroupChatCommand(
            chatRoomId = chatRoomId,
            userId = userId
        )
        val chatRoom = manageGroupChatUseCase.leaveGroup(command)
        val response = chatRoom?.let { GroupChatResponse.from(it) }

        val message = if (response != null) "그룹 채팅방을 나갔습니다." else "그룹 채팅방이 삭제되었습니다."
        return ResponseEntity.ok(ResponseDto.success(response, message))
    }
}