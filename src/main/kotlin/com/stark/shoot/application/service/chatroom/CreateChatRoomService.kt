package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.UseCase

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
        // 1) 이미 존재하는 1:1 채팅방이 있는지 확인
        val existingRooms = loadChatRoomPort.findByParticipantId(userId)
            .filter { it.participants.size == 2 && it.participants.contains(friendId) }

        // 이미 존재하는 채팅방이 있으면 반환
        if (existingRooms.isNotEmpty()) {
            return existingRooms.first()
        }



        // 2) 새 채팅방 생성을 위해 참여자 집합 구성
        val participants = setOf(userId, friendId)

        // 3) SSE 이벤트 발행 (각 참여자에게 채팅방 생성 이벤트 발행)
        publishChatRoomCreatedEvent(participants, savedRoom)

        return savedRoom
    }

    /**
     * SSE 이벤트 발행
     *
     * @param participants 참여자 목록
     * @param savedRoom 저장된 채팅방
     */
    private fun publishChatRoomCreatedEvent(
        participants: Set<Long>,
        savedRoom: ChatRoom
    ) {
        participants.forEach { participantId ->
            eventPublisher.publish(
                ChatRoomCreatedEvent(
                    roomId = savedRoom.id.toString(),
                    userId = participantId.toString()
                )
            )
        }
    }

}