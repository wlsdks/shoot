package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ChatRoomType
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatRoomPort
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
    private val saveChatRoomPort: SaveChatRoomPort
) : CreateChatRoomUseCase, ManageChatRoomUseCase {

    /**
     * @param title 채팅방 제목
     * @param participants 참여자 목록
     * @return ChatRoom 채팅방
     * @apiNote 채팅방 생성
     */
    override fun create(
        title: String?,
        participants: Set<ObjectId>
    ): ChatRoom {
        require(participants.size == 2) { "채팅방은 최소 2명 이상의 참여자가 필요합니다." }

        // todo: 근데 메타데이터의 Participant()로 만드는 데이터가 애매한데.. 그냥 깡통 데이터를 만들어서 넣는건가? 그럴 필요가 있을까
        val metadata = ChatRoomMetadata(
            title = title,
            type = if (participants.size == 2) ChatRoomType.INDIVIDUAL else ChatRoomType.GROUP,
            participantsMetadata = participants.associateWith { Participant() },
            settings = ChatRoomSettings()
        )

        val chatRoom = ChatRoom(
            participants = participants.toMutableSet(),
            metadata = metadata
        )

        return saveChatRoomPort.save(chatRoom)
    }

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
        // 1) 먼저 "이미 존재하는 1:1 채팅방" 있는지 찾는다
        val existingRooms = loadChatRoomPort.findByParticipantId(userId)
            .filter { it.participants.size == 2 && it.participants.contains(friendId) }
        // 만약 ChatRoomType.INDIVIDUAL 필드가 있다면 여기서도 체크

        if (existingRooms.isNotEmpty()) {
            // 이미 둘만의 방이 하나라도 있으면 그걸 재활용
            return existingRooms.first()
        }

        // 2) 없으면 새로 만듦
        val participants = setOf(userId, friendId)

        // [중요] metadata 파라미터를 포함해서 생성
        val metadata = ChatRoomMetadata(
            title = null,  // 1:1 채팅방 제목을 "개인채팅" or "둘 닉네임 조합" etc.
            type = ChatRoomType.INDIVIDUAL,
            participantsMetadata = participants.associateWith { Participant() },
            settings = ChatRoomSettings()
            // announcement, etc. 필요 시 여기에
        )

        val chatRoom = ChatRoom(
            participants = participants.toMutableSet(),
            metadata = metadata
            // lastMessageId, etc.는 기본값이면 생략 가능
        )

        return saveChatRoomPort.save(chatRoom)
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