package com.stark.shoot.adapter.out.persistence.mongodb.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatRoomMongoRepository
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.types.ObjectId

@Adapter
class LoadChatRoomPersistenceAdapter(
    private val chatRoomRepository: ChatRoomMongoRepository,
    private val chatRoomMapper: ChatRoomMapper
) : LoadChatRoomPort {

    /**
     * ID로 채팅방 조회
     *
     * @param id 채팅방 ID
     * @return 채팅방
     */
    override fun findById(
        id: ObjectId
    ): ChatRoom? {
        return chatRoomRepository.findById(id)
            .map(chatRoomMapper::toDomain)
            .orElse(null)
    }

    /**
     * 참여자 ID로 채팅방 조회
     *
     * @param participantId 참여자 ID
     * @return 채팅방 목록
     */
    override fun findByParticipantId(
        participantId: ObjectId
    ): List<ChatRoom> {
        return chatRoomRepository.findByParticipantsContaining(participantId)
            .map(chatRoomMapper::toDomain)
    }

}