package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.Participant
import com.stark.shoot.infrastructure.annotation.UseCase
import org.bson.types.ObjectId
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@UseCase
class FindChatroomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val loadMessagePort: LoadMessagePort
) : FindChatRoomUseCase {

    // 타임스탬프 포맷터 (예: "오후 3:15")
    private val formatter = DateTimeFormatter.ofPattern("a h:mm")

    /**
     * 사용자가 참여한 채팅방 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return ChatRoomResponse 채팅방 목록
     */
    override fun getChatRoomsForUser(
        userId: ObjectId
    ): List<ChatRoomResponse> {
        // 사용자가 참여한 채팅방 목록을 조회합니다.
        val chatRooms: List<ChatRoom> = loadChatRoomPort.findByParticipantId(userId)

        // 채팅방을 정렬합니다.
        val sortedRooms = processChatRoomSort(chatRooms, userId)

        // 정렬된 채팅방 목록을 ChatRoomResponse로 변환합니다.
        return mapToChatRoomResponses(sortedRooms, userId)
    }


    /**
     * 채팅방 정렬
     *
     * @param chatRooms 채팅방 목록
     * @param userId 사용자 ID
     * @return List<ChatRoom> 정렬된 채팅방 목록
     */
    private fun processChatRoomSort(
        chatRooms: List<ChatRoom>,
        userId: ObjectId
    ): List<ChatRoom> {
        val sortedRooms = chatRooms.sortedWith(
            compareByDescending<ChatRoom> {
                // 사용자의 정보를 찾아냅니다.
                val p = it.metadata.participantsMetadata[userId]

                // 고정 채팅방이 먼저 표시되도록 합니다.
                if (p?.isPinned == true) 1 else 0
            }.thenByDescending {
                // 고정 채팅방이 아닌 경우, 마지막 고정 시간을 기준으로 정렬합니다.
                it.metadata.participantsMetadata[userId]?.pinTimestamp ?: Instant.EPOCH
            }.thenByDescending {
                // 마지막 활동 시간을 기준으로 정렬합니다.
                it.lastActiveAt
            }
        )
        return sortedRooms
    }

    /**
     * ChatRoomResponse로 변환
     *
     * @param sortedRooms 정렬된 채팅방 목록
     * @param userId 사용자 ID
     * @return List<ChatRoomResponse> ChatRoomResponse 목록
     */
    private fun mapToChatRoomResponses(
        sortedRooms: List<ChatRoom>,
        userId: ObjectId
    ): List<ChatRoomResponse> {
        return sortedRooms.map { room ->
            // 참여자 정보 조회
            val participant = room.metadata.participantsMetadata[userId]

            // 1:1 채팅일 경우, 현재 사용자를 제외한 상대방의 닉네임을 title로 사용
            val roomTitle = createChatRoomTitle(room, userId)

            // 만약 lastMessageId가 있다면 메시지 내용을 조회 (없으면 기본 텍스트)
            val lastMessageText = createLastMessageText(room)

            // ChatRoomResponse로 변환
            mapToResponse(room, roomTitle, lastMessageText, participant)
        }
    }

    /**
     * 채팅방 제목 생성
     *
     * @param room 채팅방
     * @param userId 사용자 ID
     * @return String 채팅방 제목
     */
    private fun createChatRoomTitle(
        room: ChatRoom,
        userId: ObjectId
    ): String {
        val roomTitle =
            if (room.metadata.type.name == "INDIVIDUAL") {
                // 참여자 집합에서 현재 사용자를 제외한 다른 참여자의 ID를 찾습니다.
                val otherParticipantId = room.participants.firstOrNull { it != userId }

                // 해당 참여자의 metadata에서 nickname을 가져옵니다.
                otherParticipantId?.let { room.metadata.participantsMetadata[it]?.nickname } ?: "채팅방"
            } else {
                room.metadata.title ?: "채팅방"
            }

        return roomTitle
    }

    /**
     * 마지막 메시지 텍스트 생성
     *
     * @param room 채팅방
     * @return String 마지막 메시지 텍스트
     */
    private fun createLastMessageText(
        room: ChatRoom
    ): String {
        val lastMessageText =
            if (room.lastMessageId != null) {
                // 마지막 메시지 ID로 메시지를 조회하여 텍스트를 가져옵니다.
                val message = loadMessagePort.findById(ObjectId(room.lastMessageId))

                // 메시지가 존재하면 content.text를 반환하고, 없으면 "최근 메시지가 없습니다."를 반환합니다.
                message?.content?.text ?: "최근 메시지가 없습니다."
            } else {
                "최근 메시지가 없습니다."
            }

        return lastMessageText
    }

    /**
     * ChatRoomResponse로 변환
     *
     * @param room 채팅방
     * @param roomTitle 채팅방 제목
     * @param lastMessageText 마지막 메시지 텍스트
     * @param participant 참여자
     * @return ChatRoomResponse ChatRoomResponse 객체
     */
    private fun mapToResponse(
        room: ChatRoom,
        roomTitle: String,
        lastMessageText: String,
        participant: Participant?
    ) = ChatRoomResponse(
        roomId = room.id ?: "",
        title = roomTitle,
        lastMessage = lastMessageText,
        unreadMessages = participant?.unreadCount ?: 0,
        isPinned = participant?.isPinned ?: false,
        timestamp = room.lastActiveAt.atZone(ZoneId.systemDefault()).let { formatter.format(it) }
    )

}
