package com.stark.shoot.application.service

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ChatRoomType
import com.stark.shoot.application.port.`in`.CreateChatRoomUseCase
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomMetadata
import com.stark.shoot.domain.chat.room.ChatRoomSettings
import com.stark.shoot.domain.chat.room.Participant
import org.springframework.stereotype.Service

@Service
class ChatRoomService(
    private val saveCharRoomPort: SaveChatRoomPort
) : CreateChatRoomUseCase {

    /**
     * @param title 채팅방 제목
     * @param participants 참여자 목록
     * @return ChatRoom 채팅방
     * @apiNote 채팅방 생성
     */
    override fun create(title: String?, participants: Set<String>): ChatRoom {
        require(participants.size == 2) { "채팅방은 최소 2명 이상의 참여자가 필요합니다." }

        val metadata = ChatRoomMetadata(
            title = title,
            type = if (participants.size == 2) ChatRoomType.INDIVIDUAL else ChatRoomType.GROUP,
            participantsMetadata = participants.associateWith { Participant() },
            settings = ChatRoomSettings()
        )

        val chatRoom = ChatRoom(
            participants = participants,
            metadata = metadata
        )

        return saveCharRoomPort.save(chatRoom)
    }

}