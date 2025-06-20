package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.service.ChatRoomDomainService
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.domain.service.chatroom.ChatRoomEventService
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class CreateChatRoomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val findUserPort: FindUserPort,
    private val eventPublisher: EventPublisher,
    private val chatRoomEventService: ChatRoomEventService,
    private val chatRoomDomainService: ChatRoomDomainService,
) : CreateChatRoomUseCase {

    /**
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return ChatRoomResponse 생성된 채팅방 정보
     * @apiNote 1:1 채팅방 생성
     */
    override fun createDirectChat(
        userId: UserId,
        friendId: UserId
    ): ChatRoomResponse {
        // 1. 사용자와 친구가 존재하는지 확인
        val user = findUserPort.findUserById(userId.value)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${userId.value}")

        val friend = findUserPort.findUserById(friendId.value)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${friendId.value}")

        // 2. 이미 존재하는 1:1 채팅방이 있는지 확인 (도메인 객체의 정적 메서드 사용)
        val existingRooms = loadChatRoomPort.findByParticipantId(userId.value)
        val existingRoom = chatRoomDomainService.findDirectChatBetween(existingRooms, userId.value, friendId.value)

        // 이미 존재하는 채팅방이 있으면 반환
        if (existingRoom != null) return ChatRoomResponse.from(existingRoom, userId.value)

        // 3. 새 1:1 채팅방 생성 및 저장
        val savedRoom = registerNewDirectChatRoom(userId.value, friendId.value, friend)

        // 4. 채팅방 생성 이벤트 발행
        publishChatRoomCreatedEvent(savedRoom)

        return ChatRoomResponse.from(savedRoom, userId.value)
    }

    private fun publishChatRoomCreatedEvent(savedRoom: ChatRoom) {
        // 채팅방 생성 이벤트 발행 (도메인 서비스를 통해 처리)
        val events = chatRoomEventService.createChatRoomCreatedEvents(savedRoom)

        // 이벤트를 이벤트 퍼블리셔를 통해 발행
        events.forEach { event ->
            eventPublisher.publish(event)
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
        )

        // 채팅방 저장
        val savedRoom = saveChatRoomPort.save(newChatRoom)
        return savedRoom
    }

}
