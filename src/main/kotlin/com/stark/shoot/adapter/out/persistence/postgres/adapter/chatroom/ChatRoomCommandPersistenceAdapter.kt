package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomAnnouncement
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.orm.ObjectOptimisticLockingFailureException

@Adapter
class ChatRoomCommandPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomUserRepository: ChatRoomUserRepository,
    private val userRepository: UserRepository,
    private val chatRoomMapper: ChatRoomMapper,
) : ChatRoomCommandPort {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 100L
    }

    /**
     * 채팅방 저장 (Optimistic Lock 재시도 로직 포함)
     * 동시 업데이트 시 OptimisticLockingFailureException 발생 가능
     * 최대 3회까지 재시도하며, 지수 백오프 적용
     */
    override fun save(chatRoom: ChatRoom): ChatRoom {
        var lastException: Exception? = null

        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                return saveInternal(chatRoom)
            } catch (e: ObjectOptimisticLockingFailureException) {
                lastException = e
                if (attempt < MAX_RETRY_COUNT - 1) {
                    logger.warn { "Optimistic lock 충돌 발생, 재시도 ${attempt + 1}/$MAX_RETRY_COUNT: roomId=${chatRoom.id}" }
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1))  // 지수 백오프
                }
            }
        }

        val exception = lastException
            ?: IllegalStateException("Optimistic lock retry failed but no exception was captured")
        logger.error(exception) { "Optimistic lock 재시도 횟수 초과: roomId=${chatRoom.id}" }
        throw exception
    }

    /**
     * 실제 채팅방 저장 로직
     */
    private fun saveInternal(chatRoom: ChatRoom): ChatRoom {
        val savedChatRoomEntity = if (chatRoom.id != null) {
            val existingEntity = chatRoomRepository.findById(chatRoom.id.value).orElseThrow {
                IllegalStateException("채팅방을 찾을 수 없습니다. id=${chatRoom.id}")
            }

            existingEntity.update(
                title = chatRoom.title?.value,
                type = chatRoom.type,
                announcement = chatRoom.announcement?.value,
                lastMessageId = chatRoom.lastMessageId?.toLongOrNull(),
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

    override fun updateLastReadMessageId(
        roomId: ChatRoomId,
        userId: UserId,
        messageId: MessageId
    ) {
        chatRoomUserRepository.updateLastReadMessageId(roomId.value, userId.value, messageId.value)
    }

}
