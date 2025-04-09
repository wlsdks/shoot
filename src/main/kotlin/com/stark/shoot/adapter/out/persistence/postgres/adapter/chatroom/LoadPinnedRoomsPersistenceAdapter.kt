package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.application.port.out.chatroom.LoadPinnedRoomsPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class LoadPinnedRoomsPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomUserRepository: ChatRoomUserRepository,
    private val chatRoomMapper: ChatRoomMapper
) : LoadPinnedRoomsPort {

    /**
     * 사용자가 고정한 채팅방 목록을 조회합니다.
     * @param userId 사용자 ID (String 형식으로 전달되며, 내부에서 Long으로 변환)
     * @return 도메인 모델 ChatRoom 목록
     */
    override fun findByUserId(userId: Long): List<ChatRoom> {
        // 1. 먼저 해당 사용자가 고정한 채팅방 사용자 관계 조회
        val pinnedChatRoomUsers = chatRoomUserRepository.findByUserIdAndIsPinnedTrue(userId)

        // 2. 고정된 채팅방이 없으면 빈 리스트 반환
        if (pinnedChatRoomUsers.isEmpty()) {
            return emptyList()
        }

        // 3. 고정된 채팅방 ID 목록을 가져옴
        val pinnedRoomIds = pinnedChatRoomUsers.map { it.chatRoom.id }

        // 4. 채팅방 엔티티 목록 조회
        val chatRoomEntities = chatRoomRepository.findAllById(pinnedRoomIds)

        // 5. 각 채팅방에 대한 모든 참여자 정보 조회
        return chatRoomEntities.map { chatRoomEntity ->
            val participants = chatRoomUserRepository.findByChatRoomId(chatRoomEntity.id)
            chatRoomMapper.toDomain(chatRoomEntity, participants)
        }
    }

}