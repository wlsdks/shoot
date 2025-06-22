package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.chatroom.ChatRoomPort
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomAnnouncement
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class ChatRoomPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomUserRepository: ChatRoomUserRepository,
    private val userRepository: UserRepository,
    private val chatRoomMapper: ChatRoomMapper,
) : ChatRoomPort {

    override fun save(chatRoom: ChatRoom): ChatRoom {
        val savedChatRoomEntity = if (chatRoom.id != null) {
            val existingEntity = chatRoomRepository.findById(chatRoom.id.value).orElseThrow {
                IllegalStateException("채팅방을 찾을 수 없습니다. id=${chatRoom.id}")
            }

            existingEntity.update(
                title = chatRoom.title?.value,
                type = chatRoom.type,
                announcement = chatRoom.announcement?.value,
                lastMessageId = chatRoom.lastMessageId?.value?.toLongOrNull(),
                lastActiveAt = chatRoom.lastActiveAt
            )

            chatRoomRepository.save(existingEntity)
        } else {
            val chatRoomEntity = chatRoomMapper.toEntity(chatRoom)
            chatRoomRepository.save(chatRoomEntity)
        }

        val existingParticipants = if (chatRoom.id != null) {
            chatRoomUserRepository.findByChatRoomId(savedChatRoomEntity.id)
                .associateBy { it.user.id }
        } else {
            emptyMap()
        }

        val participantLongIds = chatRoom.participants.map { it.value }
        val allUsers = userRepository.findAllById(participantLongIds)
        val userMap = allUsers.associateBy { it.id }

        val currentParticipantIds = existingParticipants.keys

        val existingChatRoom = ChatRoom(
            id = ChatRoomId.from(savedChatRoomEntity.id),
            title = savedChatRoomEntity.title?.let { ChatRoomTitle.from(it) },
            type = savedChatRoomEntity.type,
            participants = currentParticipantIds.map { UserId.from(it) }.toMutableSet(),
            pinnedParticipants = existingParticipants.filter { it.value.isPinned }.keys.map { UserId.from(it) }
                .toMutableSet(),
            announcement = savedChatRoomEntity.announcement?.let { ChatRoomAnnouncement.from(it) }
        )

        val participantChanges = existingChatRoom.calculateParticipantChanges(
            newParticipants = chatRoom.participants,
            newPinnedParticipants = chatRoom.pinnedParticipants
        )

        participantChanges.participantsToAdd.forEach { participantId ->
            val user = userMap[participantId.value] ?: return@forEach
            val isPinned = chatRoom.pinnedParticipants.contains(participantId)

            val chatRoomUser = ChatRoomUserEntity(
                chatRoom = savedChatRoomEntity,
                user = user,
                isPinned = isPinned
            )
            chatRoomUserRepository.save(chatRoomUser)
        }

        participantChanges.pinnedStatusChanges.forEach { (participantId, isPinned) ->
            val existingUser = existingParticipants[participantId.value] ?: return@forEach

            if (existingUser.isPinned != isPinned) {
                existingUser.isPinned = isPinned
                chatRoomUserRepository.save(existingUser)
            }
        }

        if (participantChanges.participantsToRemove.isNotEmpty()) {
            val userIdsToRemove = participantChanges.participantsToRemove.map { it.value }
            chatRoomUserRepository.deleteAllByIdInBatch(
                existingParticipants.filterKeys { it in userIdsToRemove }.values.map { it.id }
            )
        }

        val updatedParticipants = chatRoomUserRepository.findByChatRoomId(savedChatRoomEntity.id)
        return chatRoomMapper.toDomain(savedChatRoomEntity, updatedParticipants)
    }

    override fun deleteById(roomId: ChatRoomId): Boolean {
        return try {
            if (!chatRoomRepository.existsById(roomId.value)) {
                return false
            }

            chatRoomRepository.deleteById(roomId.value)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun updateLastReadMessageId(roomId: ChatRoomId, userId: UserId, messageId: MessageId) {
        chatRoomUserRepository.updateLastReadMessageId(roomId.value, userId.value, messageId.value)
    }

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
        return chatRoomRepository.findAllById(chatRoomIds).map { entity ->
            val allParticipants = chatRoomUserRepository.findByChatRoomId(entity.id)
            chatRoomMapper.toDomain(entity, allParticipants)
        }
    }

    override fun findByUserId(userId: UserId): List<ChatRoom> {
        val pinnedChatRoomUsers = chatRoomUserRepository.findByUserIdAndIsPinnedTrue(userId.value)
        if (pinnedChatRoomUsers.isEmpty()) {
            return emptyList()
        }

        val pinnedRoomIds = pinnedChatRoomUsers.map { it.chatRoom.id }
        val chatRoomEntities = chatRoomRepository.findAllById(pinnedRoomIds)

        return chatRoomEntities.map { entity ->
            val participants = chatRoomUserRepository.findByChatRoomId(entity.id)
            chatRoomMapper.toDomain(entity, participants)
        }
    }
}
