package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomType
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@UseCase
class CreateChatRoomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val findUserPort: FindUserPort,
    private val eventPublisher: EventPublisher
) : CreateChatRoomUseCase {

    /**
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return ChatRoom 채팅방
     * @apiNote 1:1 채팅방 생성
     */
    override fun createDirectChat(
        userId: Long,
        friendId: Long
    ): ChatRoom {
        // 1. 사용자와 친구가 존재하는지 확인
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        val friend = findUserPort.findUserById(friendId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $friendId")

        // 2. 이미 존재하는 1:1 채팅방이 있는지 확인
        val existingRooms = loadChatRoomPort.findByParticipantId(userId)
            .filter { chatRoom -> chatRoom.isDirectChatBetween(userId, friendId) }

        // 이미 존재하는 채팅방이 있으면 반환
        if (existingRooms.isNotEmpty()) {
            return existingRooms.first()
        }

        // 3. 새 1:1 채팅방 생성 (도메인 객체의 팩토리 메서드 사용)
        val newChatRoom = ChatRoom.createDirectChat(
            userId = userId,
            friendId = friendId,
            friendName = friend.nickname
        )

        // 4. 채팅방 저장
        val savedRoom = saveChatRoomPort.save(newChatRoom)

        // 5. 이벤트 발행 (채팅방 생성 알림)
        publishChatRoomCreatedEvent(savedRoom)

        return savedRoom
    }

    /**
     * 채팅방 생성 이벤트 발행
     */
    private fun publishChatRoomCreatedEvent(chatRoom: ChatRoom) {
        // 각 참여자에게 이벤트 전송
        chatRoom.participants.forEach { participantId ->
            eventPublisher.publish(
                ChatRoomCreatedEvent(
                    roomId = chatRoom.id ?: 0L,
                    userId = participantId
                )
            )
        }
    }

}
