package com.stark.shoot.adapter.out.persistence.mongodb.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class LoadChatRoomPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomMapper: ChatRoomMapper
) : LoadChatRoomPort {

    /**
     * ID로 채팅방 조회
     *
     * @param userId 채팅방 ID
     * @return 채팅방
     */
    override fun findById(
        userId: Long
    ): ChatRoom? {
        return chatRoomRepository.findById(userId)
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
        participantId: Long
    ): List<ChatRoom> {
        return chatRoomRepository.findByParticipantIds(
            (listOf(participantId))
                .toMutableList()
        ).map(chatRoomMapper::toDomain)
    }

}