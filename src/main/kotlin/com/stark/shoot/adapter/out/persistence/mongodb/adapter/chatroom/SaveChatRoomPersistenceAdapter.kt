package com.stark.shoot.adapter.out.persistence.mongodb.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatRoomMongoRepository
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class SaveChatRoomPersistenceAdapter(
    private val chatRoomRepository: ChatRoomMongoRepository,
    private val chatRoomMapper: ChatRoomMapper
) : SaveChatRoomPort {

    /**
     * 채팅방 저장
     *
     * @param chatRoom 채팅방
     * @return 저장된 채팅방
     */
    override fun save(
        chatRoom: ChatRoom
    ): ChatRoom {
        val document = chatRoomMapper.toDocument(chatRoom)
        return chatRoomRepository.save(document)
            .let(chatRoomMapper::toDomain)
    }

}