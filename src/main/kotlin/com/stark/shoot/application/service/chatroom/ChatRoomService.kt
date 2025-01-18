package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ChatRoomType
import com.stark.shoot.application.port.`in`.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.ManageChatRoomUseCase
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomMetadata
import com.stark.shoot.domain.chat.room.ChatRoomSettings
import com.stark.shoot.domain.chat.room.Participant
import com.stark.shoot.infrastructure.common.toObjectId
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
    override fun create(title: String?, participants: Set<ObjectId>): ChatRoom {
        require(participants.size == 2) { "채팅방은 최소 2명 이상의 참여자가 필요합니다." }

        val metadata = ChatRoomMetadata(
            title = title,
            type = if (participants.size == 2) ChatRoomType.INDIVIDUAL else ChatRoomType.GROUP,
            participantsMetadata = participants.associateWith { Participant() },
            settings = ChatRoomSettings()
        )

        val chatRoom = ChatRoom(
            participants = participants.map { it.toString() }.toMutableSet(),
            metadata = metadata
        )

        return saveChatRoomPort.save(chatRoom)
    }

    override fun addParticipant(roomId: String, userId: String): Boolean {
        val chatRoom = loadChatRoomPort.findById(roomId.toObjectId())
            ?: throw IllegalArgumentException("채팅방을 찾을 수 없습니다.")
        chatRoom.participants.add(userId)
        saveChatRoomPort.save(chatRoom)
        return true
    }

    override fun removeParticipant(roomId: String, userId: String): Boolean {
        val chatRoom = loadChatRoomPort.findById(roomId.toObjectId())
            ?: throw IllegalArgumentException("채팅방을 찾을 수 없습니다.")
        chatRoom.participants.remove(userId)
        saveChatRoomPort.save(chatRoom)
        return true
    }

    override fun updateRoomSettings(roomId: String, title: String?, notificationEnabled: Boolean?) {
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

}