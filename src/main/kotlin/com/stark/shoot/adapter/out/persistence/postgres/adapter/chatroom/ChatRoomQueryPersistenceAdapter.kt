package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class ChatRoomQueryPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomUserRepository: ChatRoomUserRepository,
    private val chatRoomMapper: ChatRoomMapper,
) : ChatRoomQueryPort {

    override fun findById(roomId: ChatRoomId): ChatRoom? {
        val chatRoomEntity = chatRoomRepository.findById(roomId.value).orElse(null) ?: return null
        val participants = chatRoomUserRepository.findByChatRoomId(roomId.value)
        return chatRoomMapper.toDomain(chatRoomEntity, participants)
    }

    /**
     * 사용자가 참여 중인 채팅방 목록 조회
     * 최적화: 2번의 쿼리로 모든 데이터 조회 (기존 3번 → 2번)
     */
    override fun findByParticipantId(participantId: UserId): List<ChatRoom> {
        // 1. 채팅방 ID 목록 조회 (최근 활동 순 정렬 포함)
        val chatRoomIds = chatRoomRepository.findChatRoomIdsByUserId(participantId.value)
        if (chatRoomIds.isEmpty()) {
            return emptyList()
        }

        // 2. 채팅방 엔티티를 정렬된 순서로 배치 조회
        val chatRoomEntities = chatRoomRepository.findAllByIdOrderByLastActiveAtDesc(chatRoomIds)

        // 3. 모든 참여자를 한 번의 쿼리로 배치 조회
        val allParticipants = chatRoomUserRepository.findAllByChatRoomIds(chatRoomIds)
        val participantsByChatRoomId = allParticipants.groupBy { it.chatRoomId }

        // 4. 정렬된 순서대로 도메인 객체 생성
        return chatRoomEntities.map { entity ->
            val participants = participantsByChatRoomId[entity.id] ?: emptyList()
            chatRoomMapper.toDomain(entity, participants)
        }
    }

    /**
     * 사용자가 고정한 채팅방 목록 조회
     * 최적화: 2번의 쿼리로 모든 데이터 조회
     */
    override fun findByUserId(userId: UserId): List<ChatRoom> {
        // 1. 고정된 채팅방 사용자 조회
        val pinnedChatRoomUsers = chatRoomUserRepository.findByUserIdAndIsPinnedTrue(userId.value)
        if (pinnedChatRoomUsers.isEmpty()) {
            return emptyList()
        }

        val pinnedRoomIds = pinnedChatRoomUsers.map { it.chatRoomId }

        // 2. 채팅방 엔티티를 정렬된 순서로 배치 조회
        val chatRoomEntities = chatRoomRepository.findAllByIdOrderByLastActiveAtDesc(pinnedRoomIds)

        // 3. 모든 참여자를 한 번의 쿼리로 배치 조회
        val allParticipants = chatRoomUserRepository.findAllByChatRoomIds(pinnedRoomIds)
        val participantsByChatRoomId = allParticipants.groupBy { it.chatRoomId }

        // 4. 정렬된 순서대로 도메인 객체 생성
        return chatRoomEntities.map { entity ->
            val participants = participantsByChatRoomId[entity.id] ?: emptyList()
            chatRoomMapper.toDomain(entity, participants)
        }
    }

}
