package com.stark.shoot.adapter.`in`.socket.group

import com.stark.shoot.adapter.`in`.socket.dto.group.GroupChatActionDto
import com.stark.shoot.application.port.`in`.chatroom.group.CreateGroupChatUseCase
import com.stark.shoot.application.port.`in`.chatroom.group.ManageGroupChatUseCase
import com.stark.shoot.application.port.`in`.chatroom.group.command.CreateGroupChatCommand
import com.stark.shoot.application.port.`in`.chatroom.group.command.LeaveGroupChatCommand
import com.stark.shoot.application.port.`in`.chatroom.group.command.ManageGroupParticipantsCommand
import com.stark.shoot.application.port.`in`.chatroom.group.command.UpdateGroupChatTitleCommand
import com.stark.shoot.infrastructure.config.socket.StompPrincipal
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
class GroupChatStompHandler(
    private val createGroupChatUseCase: CreateGroupChatUseCase,
    private val manageGroupChatUseCase: ManageGroupChatUseCase
) {
    
    private val logger = KotlinLogging.logger {}

    /**
     * 그룹 채팅방 관련 WebSocket 액션 처리
     * 
     * WebSocket Endpoint: /app/group-chat
     * Protocol: STOMP over WebSocket
     */
    @MessageMapping("/group-chat")
    fun handleGroupChatAction(
        @Payload actionDto: GroupChatActionDto,
        @AuthenticationPrincipal principal: StompPrincipal
    ) {
        val userId = principal.name.toLong()
        logger.info { "Group chat action received: ${actionDto.action} from user $userId" }

        try {
            when (actionDto.action.uppercase()) {
                GroupChatActionDto.Action.CREATE.value -> {
                    handleCreateGroupChat(actionDto, userId)
                }
                
                GroupChatActionDto.Action.ADD_PARTICIPANT.value -> {
                    handleAddParticipants(actionDto, userId)
                }
                
                GroupChatActionDto.Action.REMOVE_PARTICIPANT.value -> {
                    handleRemoveParticipants(actionDto, userId)
                }
                
                GroupChatActionDto.Action.UPDATE_TITLE.value -> {
                    handleUpdateTitle(actionDto, userId)
                }
                
                GroupChatActionDto.Action.LEAVE.value -> {
                    handleLeaveGroup(actionDto, userId)
                }
                
                else -> {
                    logger.warn { "Unknown group chat action: ${actionDto.action}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to handle group chat action: ${actionDto.action}" }
            // WebSocket에서는 예외를 직접 클라이언트로 전송할 수 없으므로
            // 에러 상황을 별도 채널로 전송하거나 로깅만 수행
        }
    }

    private fun handleCreateGroupChat(actionDto: GroupChatActionDto, userId: Long) {
        val title = actionDto.title ?: throw IllegalArgumentException("제목이 필요합니다.")
        val participants = actionDto.participants ?: throw IllegalArgumentException("참여자가 필요합니다.")
        
        val command = CreateGroupChatCommand(
            title = title,
            participants = participants + userId, // 생성자 포함
            createdBy = userId
        )
        
        createGroupChatUseCase.createGroupChat(command)
        logger.info { "Group chat created via WebSocket: title=$title, participants=${participants.size + 1}" }
    }

    private fun handleAddParticipants(actionDto: GroupChatActionDto, userId: Long) {
        val chatRoomId = actionDto.chatRoomId ?: throw IllegalArgumentException("채팅방 ID가 필요합니다.")
        val participantsToAdd = actionDto.participantsToAdd ?: throw IllegalArgumentException("추가할 참여자가 필요합니다.")
        
        val command = ManageGroupParticipantsCommand.addParticipants(
            chatRoomId = chatRoomId,
            participants = participantsToAdd,
            managedBy = userId
        )
        
        manageGroupChatUseCase.manageParticipants(command)
        logger.info { "Participants added via WebSocket: roomId=$chatRoomId, count=${participantsToAdd.size}" }
    }

    private fun handleRemoveParticipants(actionDto: GroupChatActionDto, userId: Long) {
        val chatRoomId = actionDto.chatRoomId ?: throw IllegalArgumentException("채팅방 ID가 필요합니다.")
        val participantsToRemove = actionDto.participantsToRemove ?: throw IllegalArgumentException("제거할 참여자가 필요합니다.")
        
        val command = ManageGroupParticipantsCommand.removeParticipants(
            chatRoomId = chatRoomId,
            participants = participantsToRemove,
            managedBy = userId
        )
        
        manageGroupChatUseCase.manageParticipants(command)
        logger.info { "Participants removed via WebSocket: roomId=$chatRoomId, count=${participantsToRemove.size}" }
    }

    private fun handleUpdateTitle(actionDto: GroupChatActionDto, userId: Long) {
        val chatRoomId = actionDto.chatRoomId ?: throw IllegalArgumentException("채팅방 ID가 필요합니다.")
        val newTitle = actionDto.newTitle ?: throw IllegalArgumentException("새 제목이 필요합니다.")
        
        val command = UpdateGroupChatTitleCommand(
            chatRoomId = chatRoomId,
            newTitle = newTitle,
            updatedBy = userId
        )
        
        manageGroupChatUseCase.updateTitle(command)
        logger.info { "Group chat title updated via WebSocket: roomId=$chatRoomId, newTitle=$newTitle" }
    }

    private fun handleLeaveGroup(actionDto: GroupChatActionDto, userId: Long) {
        val chatRoomId = actionDto.chatRoomId ?: throw IllegalArgumentException("채팅방 ID가 필요합니다.")
        
        val command = LeaveGroupChatCommand(
            chatRoomId = chatRoomId,
            userId = userId
        )
        
        manageGroupChatUseCase.leaveGroup(command)
        logger.info { "User left group via WebSocket: roomId=$chatRoomId, userId=$userId" }
    }
}