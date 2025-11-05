package com.stark.shoot.application.service.chatroom.group

import com.stark.shoot.application.port.`in`.chatroom.group.ManageGroupChatUseCase
import com.stark.shoot.application.port.`in`.chatroom.group.command.LeaveGroupChatCommand
import com.stark.shoot.application.port.`in`.chatroom.group.command.ManageGroupParticipantsCommand
import com.stark.shoot.application.port.`in`.chatroom.group.command.UpdateGroupChatTitleCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomValidationDomainService
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.shared.event.ChatRoomParticipantChangedEvent
import com.stark.shoot.domain.shared.event.ChatRoomTitleChangedEvent
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.domain.exception.ChatRoomException
import com.stark.shoot.domain.exception.UserException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional
class ManageGroupChatService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val userQueryPort: UserQueryPort,
    private val eventPublishPort: EventPublishPort,
    private val chatRoomValidationDomainService: ChatRoomValidationDomainService
) : ManageGroupChatUseCase {

    private val logger = KotlinLogging.logger {}

    override fun manageParticipants(command: ManageGroupParticipantsCommand): ChatRoom {
        logger.info { 
            "Managing group chat participants: roomId=${command.chatRoomId}, " +
            "toAdd=${command.participantsToAdd.size}, toRemove=${command.participantsToRemove.size}"
        }

        val roomId = ChatRoomId.from(command.chatRoomId)
        val managedBy = UserId.from(command.managedBy)
        
        val chatRoom = chatRoomQueryPort.findById(roomId) 
            ?: throw ChatRoomException.NotFound( command.chatRoomId)

        // 그룹 채팅방인지 확인
        if (chatRoom.type != ChatRoomType.GROUP) {
            throw ChatRoomException.NotGroupChat()
        }

        // 관리자가 채팅방 참여자인지 확인
        if (managedBy !in chatRoom.participants) {
            throw ChatRoomException.NotParticipant("채팅방 참여자만 다른 참여자를 관리할 수 있습니다.")
        }

        // 현재 참여자 목록 생성
        val currentParticipants = chatRoom.participants.toMutableSet()
        
        // 추가할 참여자 검증 및 추가
        val participantsToAdd = command.participantsToAdd.map { UserId.from(it) }.toSet()
        participantsToAdd.forEach { userId ->
            if (!userQueryPort.existsById(userId)) {
                throw UserException.NotFound(userId.value)
            }
            if (userId in currentParticipants) {
                throw UserException.AlreadyParticipant(userId.value)
            }
        }
        
        // 제거할 참여자 검증
        val participantsToRemove = command.participantsToRemove.map { UserId.from(it) }.toSet()
        participantsToRemove.forEach { userId ->
            if (userId !in currentParticipants) {
                throw UserException.NotParticipant(userId.value)
            }
        }

        // 새로운 참여자 목록 계산
        val newParticipants = (currentParticipants + participantsToAdd) - participantsToRemove

        // 참여자 변경 검증
        chatRoomValidationDomainService.validateGroupChatParticipants(newParticipants.size)
        
        val changes = ChatRoom.ParticipantChanges(
            participantsToAdd = participantsToAdd,
            participantsToRemove = participantsToRemove
        )

        // 채팅방 참여자 업데이트
        chatRoom.updateParticipants(newParticipants)
        val updatedChatRoom = chatRoomCommandPort.save(chatRoom)

        // 참여자 변경 이벤트 발행
        if (!changes.isEmpty()) {
            val event = ChatRoomParticipantChangedEvent(
                roomId = roomId,
                participantsAdded = changes.participantsToAdd,
                participantsRemoved = changes.participantsToRemove,
                changedBy = managedBy
            )
            eventPublishPort.publishEvent(event)
        }

        logger.info { "Group chat participants updated: roomId=${roomId.value}" }
        return updatedChatRoom
    }

    override fun updateTitle(command: UpdateGroupChatTitleCommand): ChatRoom {
        logger.info { "Updating group chat title: roomId=${command.chatRoomId}, newTitle=${command.newTitle}" }

        val roomId = ChatRoomId.from(command.chatRoomId)
        val updatedBy = UserId.from(command.updatedBy)
        
        val chatRoom = chatRoomQueryPort.findById(roomId)
            ?: throw ChatRoomException.NotFound( command.chatRoomId)

        // 그룹 채팅방인지 확인
        if (chatRoom.type != ChatRoomType.GROUP) {
            throw ChatRoomException.NotGroupChat()
        }

        // 수정자가 채팅방 참여자인지 확인
        if (updatedBy !in chatRoom.participants) {
            throw ChatRoomException.NotParticipant("채팅방 참여자만 제목을 변경할 수 있습니다.")
        }

        val oldTitle = chatRoom.title?.value
        val newTitle = ChatRoomTitle.from(command.newTitle)
        
        // 제목이 실제로 변경되었는지 확인
        if (chatRoom.title?.value == newTitle.value) {
            return chatRoom
        }

        // 제목 업데이트
        chatRoom.update(title = newTitle)
        val updatedChatRoom = chatRoomCommandPort.save(chatRoom)

        // 제목 변경 이벤트 발행
        val event = ChatRoomTitleChangedEvent(
            roomId = roomId,
            oldTitle = oldTitle,
            newTitle = newTitle.value,
            changedBy = updatedBy
        )
        eventPublishPort.publishEvent(event)

        logger.info { "Group chat title updated: roomId=${roomId.value}" }
        return updatedChatRoom
    }

    override fun leaveGroup(command: LeaveGroupChatCommand): ChatRoom? {
        logger.info { "User leaving group chat: roomId=${command.chatRoomId}, userId=${command.userId}" }

        val roomId = ChatRoomId.from(command.chatRoomId)
        val userId = UserId.from(command.userId)
        
        val chatRoom = chatRoomQueryPort.findById(roomId)
            ?: throw ChatRoomException.NotFound( command.chatRoomId)

        // 그룹 채팅방인지 확인
        if (chatRoom.type != ChatRoomType.GROUP) {
            throw ChatRoomException.NotGroupChat()
        }

        // 사용자가 실제로 참여 중인지 확인
        if (userId !in chatRoom.participants) {
            throw ChatRoomException.NotParticipant("채팅방에 참여하고 있지 않습니다.")
        }

        // 참여자에서 사용자 제거
        val success = chatRoom.removeParticipant(userId)
        if (!success) {
            return chatRoom
        }

        // 빈 채팅방인지 확인하여 삭제
        if (chatRoom.participants.isEmpty()) {
            chatRoomCommandPort.deleteById(roomId)
            logger.info { "Empty group chat deleted: roomId=${roomId.value}" }
            return null
        }

        val updatedChatRoom = chatRoomCommandPort.save(chatRoom)

        // 참여자 변경 이벤트 발행
        val event = ChatRoomParticipantChangedEvent(
            roomId = roomId,
            participantsAdded = emptySet(),
            participantsRemoved = setOf(userId),
            changedBy = userId
        )
        eventPublishPort.publishEvent(event)

        logger.info { "User left group chat: roomId=${roomId.value}, userId=${userId.value}" }
        return updatedChatRoom
    }
}