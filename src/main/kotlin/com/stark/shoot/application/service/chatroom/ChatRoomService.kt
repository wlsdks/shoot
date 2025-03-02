package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ChatRoomType
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.out.EventPublisher
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomMetadata
import com.stark.shoot.domain.chat.room.ChatRoomSettings
import com.stark.shoot.domain.chat.room.Participant
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class ChatRoomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val retrieveUserPort: RetrieveUserPort,
    private val eventPublisher: EventPublisher
) : CreateChatRoomUseCase, ManageChatRoomUseCase {

    /**
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return ChatRoom 채팅방
     * @apiNote 1:1 채팅방 생성
     */
    override fun createDirectChat(
        userId: ObjectId,
        friendId: ObjectId
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

        // 3) 각 참여자의 닉네임을 조회 (여기서는 userService를 통해 가져온다고 가정)
        val currentUserNickname = retrieveUserPort.findById(userId)?.nickname  // "내이름" 같은 값 반환
        val friendNickname = retrieveUserPort.findById(friendId)?.nickname     // "친구이름" 같은 값 반환

        // 4) metadata를 생성할 때, participantsMetadata에 각 참여자의 Participant 객체에 닉네임을 포함시킵니다.
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

        // 5) 채팅방 도메인 객체 생성 (lastMessageId 등은 기본값 사용)
        val chatRoom = ChatRoom(
            participants = participants.toMutableSet(),
            metadata = metadata
        )

        // 6) 채팅방 저장
        val savedRoom = saveChatRoomPort.save(chatRoom)

        // 7) SSE 이벤트 발행 (각 참여자에게 채팅방 생성 이벤트 발행)
        participants.forEach { participantId ->
            eventPublisher.publish(
                ChatRoomCreatedEvent(
                    roomId = savedRoom.id.toString(),
                    userId = participantId.toString()
                )
            )
        }

        return savedRoom
    }

    /**
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return Boolean 참여자 추가 성공 여부
     * @apiNote 채팅방 참여자 추가
     */
    override fun addParticipant(
        roomId: String,
        userId: ObjectId
    ): Boolean {
        val chatRoom = loadChatRoomPort.findById(roomId.toObjectId())
            ?: throw IllegalArgumentException("채팅방을 찾을 수 없습니다.")

        chatRoom.participants.add(userId)
        saveChatRoomPort.save(chatRoom)
        return true
    }

    /**
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return Boolean 참여자 제거 성공 여부
     * @apiNote 채팅방 참여자 제거
     */
    override fun removeParticipant(
        roomId: String,
        userId: ObjectId
    ): Boolean {
        val chatRoom = loadChatRoomPort.findById(roomId.toObjectId())
            ?: throw IllegalArgumentException("채팅방을 찾을 수 없습니다.")

        chatRoom.participants.remove(userId)
        saveChatRoomPort.save(chatRoom)
        return true
    }

    /**
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return Boolean 퇴장 성공 여부
     * @apiNote 채팅방 퇴장
     */
    override fun updateRoomSettings(
        roomId: String,
        title: String?,
        notificationEnabled: Boolean?
    ) {
        val chatRoom = loadChatRoomPort.findById(roomId.toObjectId())
            ?: throw IllegalArgumentException("채팅방을 찾을 수 없습니다.")

        val updatedMetadata = chatRoom.metadata.copy(
            title = title ?: chatRoom.metadata.title,
            settings = chatRoom.metadata.settings.copy(
                isNotificationEnabled = notificationEnabled ?: chatRoom.metadata.settings.isNotificationEnabled
            )
        )

        val updatedChatRoom = chatRoom.copy(metadata = updatedMetadata)
        saveChatRoomPort.save(updatedChatRoom)
    }

    /**
     * @param roomId 채팅방 ID
     * @param announcement 공지사항
     * @apiNote 채팅방 공지사항 설정
     */
    override fun updateAnnouncement(
        roomId: String,
        announcement: String?
    ) {
        val chatRoom = loadChatRoomPort.findById(roomId.toObjectId())
            ?: throw IllegalArgumentException("채팅방을 찾을 수 없습니다.")

        val updatedMetadata = chatRoom.metadata.copy(announcement = announcement)
        val updatedChatRoom = chatRoom.copy(metadata = updatedMetadata)

        saveChatRoomPort.save(updatedChatRoom)
    }

}