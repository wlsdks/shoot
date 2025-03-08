package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.toObjectId
import org.bson.types.ObjectId

@UseCase
class ManageChatRoomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort
) : ManageChatRoomUseCase {

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