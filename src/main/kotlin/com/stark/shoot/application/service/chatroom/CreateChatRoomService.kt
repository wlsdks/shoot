package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomType
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomSettings
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

        // 3) 채팅방 메타데이터 생성
        val metadata = createChatRoomMetadata(userId, friendId, participants)

        // 4) 채팅방 생성
        val savedRoom = createChatRoom(participants, metadata)

        // 5) SSE 이벤트 발행 (각 참여자에게 채팅방 생성 이벤트 발행)
        publishChatRoomCreatedEvent(participants, savedRoom)

        return savedRoom
    }

    /**
     * 채팅방 메타데이터 생성
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @param participants 참여자 목록
     * @return ChatRoomMetadata 채팅방 메타데이터
     */
    private fun createChatRoomMetadata(
        userId: Long,
        friendId: Long,
        participants: Set<Long>
    ): ChatRoomMetadata {
        // 각 참여자의 닉네임을 조회 (여기서는 userService를 통해 가져온다고 가정)
        val currentUserNickname = findUserPort.findUserById(userId)?.nickname  // "내이름" 같은 값 반환
        val friendNickname = findUserPort.findUserById(friendId)?.nickname     // "친구이름" 같은 값 반환

        // metadata를 생성할 때, participantsMetadata에 각 참여자의 Participant 객체에 닉네임을 포함시킵니다.
        val metadata = ChatRoomMetadata(
            title = null,  // 1:1 채팅방은 제목 없이 상대방 닉네임을 사용
            type = ChatRoomType.INDIVIDUAL,
            participantsMetadata = participants.associateWith { id ->
                when (id) {
                    userId -> Participant(nickname = currentUserNickname)
                    friendId -> Participant(nickname = friendNickname)
                    else -> Participant()
                }
            },
            settings = ChatRoomSettings()
        )

        return metadata
    }

    /**
     * 채팅방 생성
     *
     * @param participants 참여자 목록
     * @param metadata 채팅방 메타데이터
     * @return ChatRoom 채팅방
     */
    private fun createChatRoom(
        participants: Set<Long>,
        metadata: ChatRoomMetadata
    ): ChatRoom {
        // 채팅방 도메인 객체 생성 (lastMessageId 등은 기본값 사용)
        val chatRoom = ChatRoom(
            participants = participants.toMutableSet(),
            metadata = metadata
        )

        // 채팅방 저장
        val savedRoom = saveChatRoomPort.save(chatRoom)
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