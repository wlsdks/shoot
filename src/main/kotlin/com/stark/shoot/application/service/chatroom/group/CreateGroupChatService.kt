package com.stark.shoot.application.service.chatroom.group

import com.stark.shoot.application.port.`in`.chatroom.group.CreateGroupChatUseCase
import com.stark.shoot.application.port.`in`.chatroom.group.command.CreateGroupChatCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomValidationDomainService
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.ChatRoomException
import com.stark.shoot.infrastructure.exception.UserException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional
class CreateGroupChatService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val userQueryPort: UserQueryPort,
    private val eventPublishPort: EventPublishPort,
    private val chatRoomValidationDomainService: ChatRoomValidationDomainService
) : CreateGroupChatUseCase {

    private val logger = KotlinLogging.logger {}

    override fun createGroupChat(command: CreateGroupChatCommand): ChatRoom {
        logger.info { "Creating group chat: title=${command.title}, participants=${command.participants.size}, createdBy=${command.createdBy}" }

        val participants = command.participants.map { UserId.from(it) }.toSet()
        val createdBy = UserId.from(command.createdBy)

        // 그룹 채팅 생성 규칙 검증
        chatRoomValidationDomainService.validateGroupChatParticipants(participants.size)
        if (createdBy !in participants) {
            throw ChatRoomException.CreatorNotInParticipants()
        }
        
        // 참여자 검증
        validateParticipants(participants)
        
        // 중복 채팅방 체크
        findExistingGroupChat(participants)?.let {
            throw ChatRoomException.DuplicateGroupChat()
        }

        // 채팅방 생성
        val chatRoom = ChatRoom(
            title = ChatRoomTitle.from(command.title),
            type = ChatRoomType.GROUP,
            participants = participants
        )

        val savedChatRoom = chatRoomCommandPort.save(chatRoom)

        // 채팅방 생성 이벤트 발행
        val createdEvent = ChatRoomCreatedEvent(
            roomId = savedChatRoom.id!!,
            userId = createdBy
        )
        eventPublishPort.publishEvent(createdEvent)

        logger.info { "Group chat created successfully: roomId=${savedChatRoom.id?.value}" }
        return savedChatRoom
    }

    /**
     * 참여자 검증
     */
    private fun validateParticipants(participants: Set<UserId>) {
        // 모든 참여자가 존재하는지 확인
        participants.forEach { userId ->
            if (!userQueryPort.existsById(userId)) {
                throw UserException.NotFound(userId.value)
            }
        }
    }

    /**
     * 동일한 참여자로 구성된 기존 그룹 채팅방 조회
     */
    private fun findExistingGroupChat(participants: Set<UserId>): ChatRoom? {
        return try {
            chatRoomQueryPort.findGroupChatByParticipants(participants)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to check existing group chat" }
            null
        }
    }
}