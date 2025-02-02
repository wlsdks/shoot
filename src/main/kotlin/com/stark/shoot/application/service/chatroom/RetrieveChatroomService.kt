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

    override fun getChatRoomsForUser(
        userId: ObjectId
    ): List<ChatRoomResponse> {
        val chatRooms: List<ChatRoom> = loadChatRoomPort.findByParticipantId(userId)

        return chatRooms.map { room ->
            ChatRoomResponse(
                roomId = room.id!!,
                title = room.metadata.title ?: "Untitled Room",
                lastMessage = room.lastMessageId,
                // participantsMetadata에서 직접 가져옴
                unreadMessages = room.metadata.participantsMetadata[userId]?.unreadCount ?: 0
            )
        }
    }

}