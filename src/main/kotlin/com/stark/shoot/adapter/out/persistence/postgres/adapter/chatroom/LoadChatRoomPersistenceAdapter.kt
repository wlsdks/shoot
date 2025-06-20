package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class LoadChatRoomPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomUserRepository: ChatRoomUserRepository,
    private val chatRoomMapper: ChatRoomMapper
) : LoadChatRoomPort {

    /**
     * ID로 채팅방 조회
     *
     * @param roomId 채팅방 ID
     * @return 채팅방
     */
    override fun findById(roomId: ChatRoomId): ChatRoom? {
        // 1. 채팅방 엔티티 조회
        val chatRoomEntity = chatRoomRepository.findById(roomId.value)
            .orElse(null) ?: return null

        // 2. 해당 채팅방의 모든 참여자 조회
        val participants = chatRoomUserRepository.findByChatRoomId(roomId.value)

        // 3. 도메인 객체로 변환
        return chatRoomMapper.toDomain(chatRoomEntity, participants)
    }

    /**
     * 참여자 ID로 채팅방 조회
     *
     * @param participantId 참여자 ID
     * @return 채팅방 목록
     */
    override fun findByParticipantId(participantId: UserId): List<ChatRoom> {
        // 1. 해당 사용자가 참여한 채팅방-사용자 관계 조회
        val chatRoomUsers = chatRoomUserRepository.findByUserId(participantId.value)

        // 2. 참여한 채팅방이 없으면 빈 리스트 반환
        if (chatRoomUsers.isEmpty()) {
            return emptyList()
        }

        // 3. 참여한 채팅방 ID 목록 조회
        val chatRoomIds = chatRoomUsers.map { it.chatRoom.id }

        // 4. 각 채팅방에 대한 모든 참여자 정보와 함께 도메인 객체로 변환
        return chatRoomRepository.findAllById(chatRoomIds).map { chatRoomEntity ->
            val allParticipants = chatRoomUserRepository.findByChatRoomId(chatRoomEntity.id)
            chatRoomMapper.toDomain(chatRoomEntity, allParticipants)
        }
    }

}