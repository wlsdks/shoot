package com.stark.shoot.adapter.out.persistence.mongodb.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.application.port.out.chatroom.LoadPinnedRoomsPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class LoadPinnedRoomsPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomMapper: ChatRoomMapper
) : LoadPinnedRoomsPort {

    /**
     * 사용자가 고정한 채팅방 목록을 조회합니다.
     * @param userId 사용자 ID (String 형식으로 전달되며, 내부에서 Long으로 변환)
     * @return 도메인 모델 ChatRoom 목록
     */
    override fun findByUserId(userId: Long): List<ChatRoom> {
        val entities = chatRoomRepository.findPinnedRoomsByUserId(userId)
        return entities.map { chatRoomMapper.toDomain(it) }
    }

}