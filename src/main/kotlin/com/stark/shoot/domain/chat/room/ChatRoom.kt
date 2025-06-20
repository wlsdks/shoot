package com.stark.shoot.domain.chat.room

import com.stark.shoot.domain.exception.FavoriteLimitExceededException
import com.stark.shoot.domain.chat.room.ChatRoomTitle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ChatRoom(
    val id: Long? = null,
    val title: ChatRoomTitle? = null,
    val type: ChatRoomType,
    val participants: MutableSet<Long>,
    val lastMessageId: String? = null,
    val lastActiveAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),

    // 필요한 경우에만 남길 선택적 필드
    val announcement: String? = null,
    val pinnedParticipants: MutableSet<Long> = mutableSetOf(),
    val updatedAt: Instant? = null,
) {
    /**
     * 참여자 변경 정보를 담는 데이터 클래스
     */
    data class ParticipantChanges(
        val participantsToAdd: Set<Long> = emptySet(),
        val participantsToRemove: Set<Long> = emptySet(),
        val pinnedStatusChanges: Map<Long, Boolean> = emptyMap()
    ) {
        fun isEmpty(): Boolean {
            return participantsToAdd.isEmpty() &&
                    participantsToRemove.isEmpty() &&
                    pinnedStatusChanges.isEmpty()
        }
    }

    /**
     * 현재 참여자 목록과 새 참여자 목록을 비교하여 변경 사항을 계산
     *
     * @param newParticipants 새 참여자 목록
     * @param newPinnedParticipants 새 고정 참여자 목록
     * @return 참여자 변경 정보
     */
    fun calculateParticipantChanges(
        newParticipants: Set<Long>,
        newPinnedParticipants: Set<Long> = this.pinnedParticipants
    ): ParticipantChanges {
        // 추가할 참여자 (새로운 참여자)
        val participantsToAdd = newParticipants - this.participants.toSet()

        // 제거할 참여자 (더 이상 참여하지 않는 사용자)
        val participantsToRemove = this.participants.toSet() - newParticipants

        // 핀 상태가 변경된 참여자
        val pinnedStatusChanges = mutableMapOf<Long, Boolean>()

        // 새 참여자 중 핀 상태 확인
        (this.participants.toSet() intersect newParticipants).forEach { participantId ->
            val wasPinned = this.pinnedParticipants.contains(participantId)
            val isPinned = newPinnedParticipants.contains(participantId)

            if (wasPinned != isPinned) {
                pinnedStatusChanges[participantId] = isPinned
            }
        }

        // 새로 추가되는 참여자 중 핀 상태인 참여자 추가
        participantsToAdd.forEach { participantId ->
            if (newPinnedParticipants.contains(participantId)) {
                pinnedStatusChanges[participantId] = true
            }
        }

        return ParticipantChanges(
            participantsToAdd = participantsToAdd,
            participantsToRemove = participantsToRemove,
            pinnedStatusChanges = pinnedStatusChanges
        )
    }

    companion object {
        private const val MAX_PINNED = 5

        // 타임스탬프 포맷터 (예: "오후 3:15")
        private val formatter = DateTimeFormatter.ofPattern("a h:mm")

        /**
         * 1:1 채팅방 생성
         *
         * @param userId 사용자 ID
         * @param friendId 친구 ID
         * @param friendName 친구 이름 (채팅방 제목용)
         * @return 새로운 1:1 채팅방
         */
        fun createDirectChat(
            userId: Long,
            friendId: Long,
            friendName: String
        ): ChatRoom {
            val title = ChatRoomTitle.from("${friendName}님과의 대화")

            return ChatRoom(
                title = title,
                type = ChatRoomType.INDIVIDUAL,
                announcement = null,
                participants = mutableSetOf(userId, friendId),
                pinnedParticipants = mutableSetOf(),
                lastMessageId = null,
                lastActiveAt = Instant.now(),
                createdAt = Instant.now()
            )
        }
    }

    /**
     * 채팅방 정보 업데이트
     */
    fun update(
        id: Long? = this.id,
        title: ChatRoomTitle? = this.title,
        type: ChatRoomType = this.type,
        announcement: String? = this.announcement,
        lastMessageId: String? = this.lastMessageId,
        lastActiveAt: Instant = this.lastActiveAt
    ): ChatRoom {
        return this.copy(
            id = id,
            title = title,
            type = type,
            announcement = announcement,
            lastMessageId = lastMessageId,
            lastActiveAt = lastActiveAt,
            updatedAt = Instant.now()
        )
    }

    /**
     * 참여자 추가
     *
     * @param userId 추가할 사용자 ID
     * @return 업데이트된 ChatRoom 객체 (이미 참여 중인 경우 현재 객체 반환)
     */
    fun addParticipant(userId: Long): ChatRoom {
        // 이미 참여 중인지 확인
        if (participants.contains(userId)) {
            return this
        }

        val updatedParticipants = this.participants.toMutableSet()
        updatedParticipants.add(userId)
        return this.copy(
            participants = updatedParticipants,
            updatedAt = Instant.now()
        )
    }

    /**
     * 참여자 제거
     *
     * @param userId 제거할 사용자 ID
     * @return 업데이트된 ChatRoom 객체 (참여자가 아닌 경우 현재 객체 반환)
     */
    fun removeParticipant(userId: Long): ChatRoom {
        // 참여자가 아닌 경우
        if (!participants.contains(userId)) {
            return this
        }

        val updatedParticipants = this.participants.toMutableSet()
        updatedParticipants.remove(userId)
        return this.copy(
            participants = updatedParticipants,
            updatedAt = Instant.now()
        )
    }

    /**
     * 여러 참여자 추가
     */
    fun addParticipants(userIds: Collection<Long>): ChatRoom {
        val updatedParticipants = this.participants.toMutableSet()
        updatedParticipants.addAll(userIds)
        return this.copy(participants = updatedParticipants)
    }

    /**
     * 여러 참여자 제거
     */
    fun removeParticipants(userIds: Collection<Long>): ChatRoom {
        val updatedParticipants = this.participants.toMutableSet()
        updatedParticipants.removeAll(userIds.toSet())
        return this.copy(participants = updatedParticipants)
    }

    /**
     * 참여자 목록 업데이트 (기존 참여자 유지하고 새 참여자 추가, 제외된 참여자 제거)
     */
    fun updateParticipants(newParticipants: Collection<Long>): ChatRoom {
        val newParticipantsSet = newParticipants.toSet()
        val participantsToAdd = newParticipantsSet - this.participants
        val participantsToRemove = this.participants - newParticipantsSet

        var updatedChatRoom = this

        if (participantsToAdd.isNotEmpty()) {
            updatedChatRoom = updatedChatRoom.addParticipants(participantsToAdd)
        }

        if (participantsToRemove.isNotEmpty()) {
            updatedChatRoom = updatedChatRoom.removeParticipants(participantsToRemove)
        }

        return updatedChatRoom
    }

    /**
     * 즐겨찾기(핀) 상태 업데이트
     *
     * @param userId 사용자 ID
     * @param isFavorite 즐겨찾기 여부
     * @param userPinnedRoomsCount 사용자가 현재 핀한 채팅방 수
     * @return 업데이트된 ChatRoom 객체
     */
    fun updateFavoriteStatus(
        userId: Long,
        isFavorite: Boolean,
        userPinnedRoomsCount: Int
    ): ChatRoom {
        val updatedPinned = updatePinnedParticipants(userId, isFavorite, userPinnedRoomsCount)
        return this.copy(pinnedParticipants = updatedPinned)
    }

    /**
     * 고정 참여자 목록 업데이트
     *
     * @param userId 사용자 ID
     * @param isFavorite 즐겨찾기 여부
     * @param userPinnedRoomsCount 사용자가 현재 핀한 채팅방 수 (현재 채팅방 제외)
     * @return 업데이트된 고정 참여자 목록
     */
    private fun updatePinnedParticipants(
        userId: Long,
        isFavorite: Boolean,
        userPinnedRoomsCount: Int
    ): MutableSet<Long> {
        val currentPinned = this.pinnedParticipants.toMutableSet()
        val isAlreadyPinned = currentPinned.contains(userId)

        // 이미 즐겨찾기된 채팅방을 다시 즐겨찾기하려고 하면 제거 (토글 동작)
        if (isFavorite && isAlreadyPinned) {
            currentPinned.remove(userId)
        }
        // 즐겨찾기 추가 요청이고 아직 즐겨찾기되지 않은 경우
        else if (isFavorite && !isAlreadyPinned) {
            if (userPinnedRoomsCount >= MAX_PINNED) {
                throw FavoriteLimitExceededException("최대 핀 채팅방 개수를 초과했습니다. (MAX_PINNED=$MAX_PINNED)")
            }
            currentPinned.add(userId)
        }
        // 즐겨찾기 해제 요청
        else if (!isFavorite) {
            currentPinned.remove(userId)
        }

        return currentPinned
    }

    /**
     * 채팅방 공지사항 업데이트
     *
     * @param announcement 새 공지사항 (null인 경우 공지사항 삭제)
     * @return 업데이트된 ChatRoom 객체
     */
    fun updateAnnouncement(announcement: String?): ChatRoom {
        return this.copy(
            announcement = announcement,
            updatedAt = Instant.now()
        )
    }

    /**
     * 채팅방이 비어있는지 확인 (참여자가 없는지)
     *
     * @return 채팅방이 비어있으면 true, 아니면 false
     */
    fun isEmpty(): Boolean {
        return participants.isEmpty()
    }

    /**
     * 채팅방이 삭제되어야 하는지 확인
     * 현재는 참여자가 없는 경우에만 삭제 대상으로 판단하지만,
     * 추후 다른 비즈니스 규칙이 추가될 수 있음
     *
     * @return 삭제되어야 하면 true, 아니면 false
     */
    fun shouldBeDeleted(): Boolean {
        return isEmpty()
    }

    /**
     * 1:1 채팅방인지 확인하고 특정 두 사용자만 포함하는지 확인
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 1:1 채팅방이고 정확히 해당 두 사용자만 포함하면 true, 아니면 false
     */
    fun isDirectChatBetween(userId1: Long, userId2: Long): Boolean {
        return type == ChatRoomType.INDIVIDUAL &&
                participants.size == 2 &&
                participants.contains(userId1) &&
                participants.contains(userId2)
    }

    /**
     * 채팅방 제목 생성
     *
     * @param userId 사용자 ID
     * @return 채팅방 제목
     */
    fun createChatRoomTitle(userId: Long): String {
        return if (ChatRoomType.INDIVIDUAL == type) {
            // 1:1 채팅인 경우, 상대방 사용자의 이름을 제목으로 설정
            val otherParticipantId = participants.find { it != userId }
            if (otherParticipantId != null) {
                // 실제 구현에서는 사용자 정보 조회 서비스를 통해 닉네임 가져오기
                title?.value ?: "1:1 채팅방"
            } else {
                title?.value ?: "1:1 채팅방"
            }
        } else {
            // 그룹 채팅의 경우 정해진 제목 사용
            title?.value ?: "그룹 채팅방"
        }
    }

    /**
     * 마지막 메시지 텍스트 생성
     *
     * @return 마지막 메시지 텍스트
     */
    fun createLastMessageText(): String {
        return if (lastMessageId != null) {
            try {
                // 마지막 메시지 ID가 있는 경우, 해당 메시지 내용 조회
                // 실제 구현에서는 메시지 저장소에서 해당 ID의 메시지 조회
                "최근 메시지" // 실제 구현시 메시지 조회 후 내용 반환
            } catch (e: Exception) {
                "메시지 조회 실패"
            }
        } else {
            "최근 메시지가 없습니다."
        }
    }

    /**
     * 채팅방의 타임스탬프 포맷팅
     *
     * @return 포맷팅된 타임스탬프
     */
    fun formatTimestamp(): String {
        return lastActiveAt.atZone(ZoneId.systemDefault()).format(formatter)
    }

}
