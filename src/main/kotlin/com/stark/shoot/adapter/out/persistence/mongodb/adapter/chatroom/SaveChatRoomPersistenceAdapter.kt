package com.stark.shoot.adapter.out.persistence.mongodb.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class SaveChatRoomPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository,
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
        val chatRoomEntity = chatRoomMapper.toEntity(chatRoom)
        return chatRoomRepository.save(chatRoomEntity)
            .let(chatRoomMapper::toDomain)
    }

}