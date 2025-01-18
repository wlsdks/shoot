package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.RetrieveChatRoomUseCase
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class RetrieveChatroomService(
    private val loadChatRoomPort: LoadChatRoomPort
) : RetrieveChatRoomUseCase {

    override fun getChatRoomsForUser(userId: ObjectId): List<ChatRoomResponse> {
        // 사용자가 참여한 채팅방 목록을 가져옴
        val chatRooms: List<ChatRoom> = loadChatRoomPort.findByParticipantId(userId)

        // 응답 DTO 구성
        return chatRooms.map { room ->
            val unreadMessages = getUnreadCount(room, userId)
            ChatRoomResponse(
                roomId = room.id!!,
                title = room.metadata.title ?: "Untitled Room",
                lastMessage = room.lastMessageId, // 실제 메시지 내용을 가져오려면 다른 로직이 필요
                unreadMessages = unreadMessages
            )
        }
    }

    private fun getUnreadCount(chatRoom: ChatRoom, userId: ObjectId): Int {
        // participantId는 ObjectId, userId도 ObjectId
        val participantDoc = chatRoom.metadata.participantsMetadata[userId]
        return participantDoc?.unreadCount ?: 0
    }

}