package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.ChatRoomSearchUseCase
import com.stark.shoot.application.port.out.LoadChatRoomPort
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class ChatRoomSearchService(
    private val loadChatRoomPort: LoadChatRoomPort,
) : ChatRoomSearchUseCase {

    override fun searchChatRooms(
        userId: ObjectId,
        query: String?,
        type: String?,
        unreadOnly: Boolean?
    ): List<ChatRoomResponse> {
        val chatRooms = loadChatRoomPort.findByParticipantId(userId)
        val filteredRooms = chatRooms.filter { room ->
            (query.isNullOrBlank() || (room.metadata.title?.contains(query, ignoreCase = true) ?: false)) &&
                    (type.isNullOrBlank() || room.metadata.type.name.equals(type, ignoreCase = true)) &&
                    (unreadOnly != true || (room.metadata.participantsMetadata[userId]?.unreadCount ?: 0) > 0)
        }

        // 응답 데이터로 변환
        return filteredRooms.map { room ->
            ChatRoomResponse(
                roomId = room.id!!,
                title = room.metadata.title ?: "Untitled Room",
                lastMessage = room.lastMessageId,
                unreadMessages = room.metadata.participantsMetadata[userId]?.unreadCount ?: 0,
                isPinned = room.metadata.participantsMetadata[userId]?.isPinned ?: false
            )
        }
    }

}