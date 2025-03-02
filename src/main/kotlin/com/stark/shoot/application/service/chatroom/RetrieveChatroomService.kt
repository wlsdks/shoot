package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.RetrieveChatRoomUseCase
import com.stark.shoot.application.port.out.message.LoadChatMessagePort
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class RetrieveChatroomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val loadChatMessagePort: LoadChatMessagePort  // 주입받음
) : RetrieveChatRoomUseCase {

    // 타임스탬프 포맷터 (예: "오후 3:15")
    private val formatter = DateTimeFormatter.ofPattern("a h:mm")

    /**
     * 사용자가 참여한 채팅방 목록을 조회합니다.
     */
    override fun getChatRoomsForUser(userId: ObjectId): List<ChatRoomResponse> {
        val chatRooms: List<ChatRoom> = loadChatRoomPort.findByParticipantId(userId)

        val sortedRooms = chatRooms.sortedWith(
            compareByDescending<ChatRoom> {
                val p = it.metadata.participantsMetadata[userId]
                if (p?.isPinned == true) 1 else 0
            }.thenByDescending {
                it.metadata.participantsMetadata[userId]?.pinTimestamp ?: Instant.EPOCH
            }.thenByDescending {
                it.lastActiveAt
            }
        )

        return sortedRooms.map { room ->
            val participant = room.metadata.participantsMetadata[userId]
            // 1:1 채팅일 경우, 현재 사용자를 제외한 상대방의 닉네임을 title로 사용
            val roomTitle = if (room.metadata.type.name == "INDIVIDUAL") {
                // 참여자 집합에서 현재 사용자를 제외한 다른 참여자의 ID를 찾습니다.
                val otherParticipantId = room.participants.firstOrNull { it != userId }
                // 해당 참여자의 metadata에서 nickname을 가져옵니다.
                otherParticipantId?.let { room.metadata.participantsMetadata[it]?.nickname } ?: "채팅방"
            } else {
                room.metadata.title ?: "채팅방"
            }

            // 만약 lastMessageId가 있다면 메시지 내용을 조회 (없으면 기본 텍스트)
            val lastMessageText = if (room.lastMessageId != null) {
                val message = loadChatMessagePort.findById(ObjectId(room.lastMessageId))
                message?.content?.text ?: "최근 메시지가 없습니다."
            } else {
                "최근 메시지가 없습니다."
            }
            ChatRoomResponse(
                roomId = room.id ?: "",
                title = roomTitle,
                lastMessage = lastMessageText,
                unreadMessages = participant?.unreadCount ?: 0,
                isPinned = participant?.isPinned ?: false,
                timestamp = room.lastActiveAt.atZone(ZoneId.systemDefault()).let { formatter.format(it) }
            )
        }
    }

}
