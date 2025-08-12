package com.stark.shoot.application.service.chatroom.group

import com.stark.shoot.application.port.`in`.chatroom.group.CreateGroupChatUseCase
import com.stark.shoot.application.port.`in`.chatroom.group.command.CreateGroupChatCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional
class CreateGroupChatService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val userQueryPort: UserQueryPort,
    private val eventPublishPort: EventPublishPort
) : CreateGroupChatUseCase {

    private val logger = KotlinLogging.logger {}

    override fun createGroupChat(command: CreateGroupChatCommand): ChatRoom {
        logger.info { "Creating group chat: title=${command.title}, participants=${command.participants.size}, createdBy=${command.createdBy}" }

        val participants = command.participants.map { UserId.from(it) }.toSet()
        val createdBy = UserId.from(command.createdBy)

        // 그룹 채팅 생성 규칙 검증
        require(participants.size >= 2) { "그룹 채팅방은 최소 2명의 참여자가 필요합니다." }
        require(participants.size <= 100) { "그룹 채팅방은 최대 100명까지 참여할 수 있습니다." }
        require(createdBy in participants) { "채팅방 생성자는 참여자에 포함되어야 합니다." }
        
        // 참여자 검증
        validateParticipants(participants)
        
        // 중복 채팅방 체크
        findExistingGroupChat(participants)?.let {
            throw IllegalStateException("동일한 참여자로 구성된 그룹 채팅방이 이미 존재합니다.")
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
                throw IllegalArgumentException("존재하지 않는 사용자입니다: ${userId.value}")
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