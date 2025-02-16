package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.RetrieveChatRoomUseCase
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RetrieveChatroomService(
    private val loadChatRoomPort: LoadChatRoomPort
) : RetrieveChatRoomUseCase {

    /**
     * 사용자가 참여한 채팅방 목록을 조회합니다.
     */
    override fun getChatRoomsForUser(
        userId: ObjectId
    ): List<ChatRoomResponse> {
        // 사용자가 참여한 채팅방 목록을 조회
        val chatRooms: List<ChatRoom> = loadChatRoomPort.findByParticipantId(userId)

        // 정렬: 핀 여부(핀:1, 아니면:0) 내림차순, 핀된 경우 pinTimestamp 내림차순, 그 외에는 마지막 활동시간 내림차순
        val sortedRooms = chatRooms.sortedWith(compareByDescending<ChatRoom> {
            val p = it.metadata.participantsMetadata[userId]
            if (p?.isPinned == true) 1 else 0
        }.thenByDescending {
            it.metadata.participantsMetadata[userId]?.pinTimestamp ?: Instant.EPOCH
        }.thenByDescending {
            it.lastActiveAt
        })

        // 정렬된 채팅방 목록을 ChatRoomResponse로 변환
        return sortedRooms.map { ChatRoomResponse.from(it, userId) }
    }

}