package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomAnnouncement
import com.stark.shoot.domain.chat.room.ChatRoomId
import com.stark.shoot.domain.chat.room.ChatRoomTitle
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class SaveChatRoomPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomUserRepository: ChatRoomUserRepository,
    private val userRepository: UserRepository,
    private val chatRoomMapper: ChatRoomMapper
) : SaveChatRoomPort {

    /**
     * 채팅방 저장
     *
     * @param chatRoom 채팅방
     * @return 저장된 채팅방
     */
    override fun save(chatRoom: ChatRoom): ChatRoom {
        // 1. 채팅방 엔티티로 변환 및 저장
        val savedChatRoomEntity = if (chatRoom.id != null) {
            // 기존 채팅방 업데이트
            val existingEntity = chatRoomRepository.findById(chatRoom.id.value).orElseThrow {
                IllegalStateException("채팅방을 찾을 수 없습니다. id=${chatRoom.id}")
            }

            // 업데이트된 도메인 객체를 사용하여 엔티티 업데이트
            existingEntity.update(
                title = chatRoom.title?.value,
                type = chatRoom.type,
                announcement = chatRoom.announcement?.value,
                lastMessageId = chatRoom.lastMessageId?.value?.toLongOrNull(),
                lastActiveAt = chatRoom.lastActiveAt
            )

            chatRoomRepository.save(existingEntity)
        } else {
            // 새 채팅방 생성
            val chatRoomEntity = chatRoomMapper.toEntity(chatRoom)
            chatRoomRepository.save(chatRoomEntity)
        }

        // 2. 기존 채팅방 참여자 조회 (ID 있는 경우 업데이트 시나리오)
        val existingParticipants = if (chatRoom.id != null) {
            chatRoomUserRepository.findByChatRoomId(savedChatRoomEntity.id)
                .associateBy { it.user.id }
        } else {
            emptyMap()
        }

        // 3. 참여자 정보 처리를 위한 사용자 조회
        val participantLongIds = chatRoom.participants.map { it.value }
        val allUsers = userRepository.findAllById(participantLongIds)
        val userMap = allUsers.associateBy { it.id }

        // 4. 참여자 정보 처리
        // 현재 참여자 ID 목록
        val currentParticipantIds = existingParticipants.keys

        // 도메인 객체를 사용하여 참여자 변경 사항 계산
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

        // 새 참여자 추가
        participantChanges.participantsToAdd.forEach { participantId ->
            val user = userMap[participantId.value] ?: return@forEach // 사용자가 없으면 건너뜀
            val isPinned = chatRoom.pinnedParticipants.contains(participantId)

            val chatRoomUser = ChatRoomUserEntity(
                chatRoom = savedChatRoomEntity,
                user = user,
                isPinned = isPinned
            )
            chatRoomUserRepository.save(chatRoomUser)
        }

        // 기존 참여자 중 핀 상태가 변경된 경우 업데이트
        participantChanges.pinnedStatusChanges.forEach { (participantId, isPinned) ->
            val existingUser = existingParticipants[participantId.value] ?: return@forEach

            if (existingUser.isPinned != isPinned) {
                existingUser.isPinned = isPinned
                chatRoomUserRepository.save(existingUser)
            }
        }

        // 참여자 제거
        if (participantChanges.participantsToRemove.isNotEmpty()) {
            val userIdsToRemove = participantChanges.participantsToRemove.map { it.value }
            chatRoomUserRepository.deleteAllByIdInBatch(
                existingParticipants.filterKeys { it in userIdsToRemove }.values.map { it.id }
            )
        }

        // 5. 업데이트된 참여자 정보와 함께 도메인 객체 반환
        val updatedParticipants = chatRoomUserRepository.findByChatRoomId(savedChatRoomEntity.id)
        return chatRoomMapper.toDomain(savedChatRoomEntity, updatedParticipants)
    }
}