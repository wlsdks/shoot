package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.RetrieveChatRoomUseCase
import com.stark.shoot.application.port.out.LoadChatMessagePort
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.common.toObjectId
import org.springframework.stereotype.Service

@Service
class RetrieveChatroomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val loadChatMessagePort: LoadChatMessagePort
) : RetrieveChatRoomUseCase {

    override fun getChatRoomsForUser(userId: String): List<ChatRoomResponse> {
        // 사용자가 참여한 채팅방 목록을 가져옴
        val chatRooms: List<ChatRoom> = loadChatRoomPort.findByParticipantId(userId.toObjectId())

        // 채팅방 목록을 가져온 후 읽지 않은 메시지 수를 계산
        return chatRooms.map { room ->
            val unreadMessages = calculateUnreadMessages(room, userId)
            room.metadata.title?.let {
                ChatRoomResponse(
                    roomId = room.id!!,
                    title = it,
                    lastMessage = room.lastMessageId, // 여기선 메시지 내용도 필요할 수 있음
                    unreadMessages = unreadMessages
                )
            }!!
        }
    }

    private fun calculateUnreadMessages(chatRoom: ChatRoom, userId: String): Int {
        val lastReadMessageId = chatRoom.metadata.participantsMetadata[userId]?.lastReadMessageId
        return loadChatMessagePort.countUnreadMessages(chatRoom.id!!, lastReadMessageId)
    }

}