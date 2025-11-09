package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.CreateDirectChatCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.domain.chatroom.service.ChatRoomEventService
import com.stark.shoot.domain.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.redis.RedisLockManager
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class CreateChatRoomService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val userQueryPort: UserQueryPort,
    private val eventPublisher: EventPublishPort,
    private val chatRoomEventService: ChatRoomEventService,
    private val chatRoomDomainService: ChatRoomDomainService,
    private val redisLockManager: RedisLockManager,
) : CreateChatRoomUseCase {

    /**
     * 1:1 채팅방 생성
     * Race Condition 방지를 위해 분산 락 적용
     *
     * @param command 직접 채팅 생성 커맨드
     * @return ChatRoomResponse 생성된 채팅방 정보
     */
    override fun createDirectChat(command: CreateDirectChatCommand): ChatRoomResponse {
        val userId = command.userId
        val friendId = command.friendId

        // 분산 락 키 생성: 두 사용자 ID를 정렬하여 A→B, B→A 요청에 대해 동일한 락 사용
        val sortedIds = listOf(userId.value, friendId.value).sorted()
        val lockKey = "chatroom:direct:${sortedIds[0]}:${sortedIds[1]}"

        // 분산 락을 획득하여 동시 채팅방 생성 방지
        return redisLockManager.withLock(lockKey, userId.value.toString()) {
            // 1. 사용자와 친구가 존재하는지 확인
            userQueryPort.findUserById(userId)
                ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${userId.value}")

            val friend = userQueryPort.findUserById(friendId)
                ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${friendId.value}")

            // 2. 이미 존재하는 1:1 채팅방이 있는지 확인 (도메인 객체의 정적 메서드 사용)
            val existingRooms = chatRoomQueryPort.findByParticipantId(userId)
            val existingRoom = chatRoomDomainService.findDirectChatBetween(existingRooms, userId, friendId)

            // 이미 존재하는 채팅방이 있으면 반환
            if (existingRoom != null) return@withLock ChatRoomResponse.from(existingRoom, userId.value)

            // 3. 새 1:1 채팅방 생성 및 저장
            val savedRoom = registerNewDirectChatRoom(userId.value, friendId.value, friend)

            // 4. 채팅방 생성 이벤트 발행
            publishChatRoomCreatedEvent(savedRoom)

            ChatRoomResponse.from(savedRoom, userId.value)
        }
    }

    private fun publishChatRoomCreatedEvent(savedRoom: ChatRoom) {
        // 채팅방 생성 이벤트 발행 (도메인 서비스를 통해 처리)
        val events = chatRoomEventService.createChatRoomCreatedEvents(savedRoom)

        // 이벤트를 이벤트 퍼블리셔를 통해 발행
        events.forEach { event ->
            eventPublisher.publishEvent(event)
        }
    }

    private fun registerNewDirectChatRoom(
        userId: Long,
        friendId: Long,
        friend: User
    ): ChatRoom {
        // 새 1:1 채팅방 생성 (도메인 객체의 팩토리 메서드 사용)
        val newChatRoom = ChatRoom.createDirectChat(
            userId = userId,
            friendId = friendId,
            friendName = friend.nickname.value
        ) { _, _ ->
            // 사용자 존재 검증은 이미 위에서 수행했으므로 패스
            // 필요시 추가 비즈니스 규칙 검증 가능
        }

        // 채팅방 저장
        val savedRoom = chatRoomCommandPort.save(newChatRoom)
        return savedRoom
    }

}
