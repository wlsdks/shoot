package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
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
        val chatRoomEntity = chatRoomMapper.toEntity(chatRoom)
        val savedChatRoomEntity = chatRoomRepository.save(chatRoomEntity)

        // 2. 기존 채팅방 참여자 조회 (ID 있는 경우 업데이트 시나리오)
        val existingParticipants = if (chatRoom.id != null) {
            chatRoomUserRepository.findByChatRoomId(savedChatRoomEntity.id)
                .associateBy { it.user.id }
        } else {
            emptyMap()
        }

        // 3. 참여자 정보 업데이트/추가
        val allUsers = userRepository.findAllById(chatRoom.participants)
        val userMap = allUsers.associateBy { it.id }

        // 4. 도메인 모델의 참여자 정보 처리
        chatRoom.participants.forEach { participantId ->
            val user = userMap[participantId] ?: return@forEach // 사용자가 없으면 건너뜀

            val isPinned = chatRoom.pinnedParticipants.contains(participantId)
            val existingUser = existingParticipants[participantId]

            if (existingUser == null) {
                // 새 참여자 추가
                val chatRoomUser = ChatRoomUserEntity(
                    chatRoom = savedChatRoomEntity,
                    user = user,
                    isPinned = isPinned
                )
                chatRoomUserRepository.save(chatRoomUser)
            } else if (existingUser.isPinned != isPinned) {
                // 고정 상태만 변경
                existingUser.isPinned = isPinned
                chatRoomUserRepository.save(existingUser)
            }
        }

        // 5. 도메인 모델에서 제외된 참여자 삭제 (선택적)
        val participantsToRemove = existingParticipants.keys - chatRoom.participants.toSet()
        if (participantsToRemove.isNotEmpty()) {
            chatRoomUserRepository.deleteAllByIdInBatch(
                existingParticipants.filterKeys { it in participantsToRemove }.values.map { it.id }
            )
        }

        // 6. 업데이트된 참여자 정보와 함께 도메인 객체 반환
        val updatedParticipants = chatRoomUserRepository.findByChatRoomId(savedChatRoomEntity.id)
        return chatRoomMapper.toDomain(savedChatRoomEntity, updatedParticipants)
    }

}