package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
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

    override fun findByParticipantId(participantId: UserId): List<ChatRoom> {
        val chatRoomUsers = chatRoomUserRepository.findByUserId(participantId.value)
        if (chatRoomUsers.isEmpty()) {
            return emptyList()
        }

        val chatRoomIds = chatRoomUsers.map { it.chatRoom.id }
        val chatRoomEntities = chatRoomRepository.findAllById(chatRoomIds)

        // N+1 방지: 모든 채팅방의 참여자를 한 번의 쿼리로 조회
        val allParticipants = chatRoomUserRepository.findAllByChatRoomIds(chatRoomIds)
        val participantsByChatRoomId = allParticipants.groupBy { it.chatRoom.id }

        return chatRoomEntities.map { entity ->
            val participants = participantsByChatRoomId[entity.id] ?: emptyList()
            chatRoomMapper.toDomain(entity, participants)
        }
    }

    override fun findByUserId(userId: UserId): List<ChatRoom> {
        val pinnedChatRoomUsers = chatRoomUserRepository.findByUserIdAndIsPinnedTrue(userId.value)
        if (pinnedChatRoomUsers.isEmpty()) {
            return emptyList()
        }

        val pinnedRoomIds = pinnedChatRoomUsers.map { it.chatRoom.id }
        val chatRoomEntities = chatRoomRepository.findAllById(pinnedRoomIds)

        // N+1 방지: 모든 채팅방의 참여자를 한 번의 쿼리로 조회
        val allParticipants = chatRoomUserRepository.findAllByChatRoomIds(pinnedRoomIds)
        val participantsByChatRoomId = allParticipants.groupBy { it.chatRoom.id }

        return chatRoomEntities.map { entity ->
            val participants = participantsByChatRoomId[entity.id] ?: emptyList()
            chatRoomMapper.toDomain(entity, participants)
        }
    }

}
