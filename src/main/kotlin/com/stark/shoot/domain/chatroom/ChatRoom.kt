package com.stark.shoot.domain.chatroom

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomAnnouncement
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.FavoriteLimitExceededException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ChatRoom(
    val id: ChatRoomId? = null,
    var title: ChatRoomTitle? = null,
    val type: ChatRoomType,
    var participants: Set<UserId>,
    var lastMessageId: MessageId? = null,
    var lastActiveAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),

    // 필요한 경우에만 남길 선택적 필드
    var announcement: ChatRoomAnnouncement? = null,
    var pinnedParticipants: Set<UserId> = emptySet(),
    var updatedAt: Instant? = null,
) {
    /**
     * 참여자 변경 정보를 담는 데이터 클래스
     */
    data class ParticipantChanges(
        val participantsToAdd: Set<UserId> = emptySet(),
        val participantsToRemove: Set<UserId> = emptySet(),
        val pinnedStatusChanges: Map<UserId, Boolean> = emptyMap()
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
        newParticipants: Set<UserId>,
        newPinnedParticipants: Set<UserId> = this.pinnedParticipants
    ): ParticipantChanges {
        // 추가할 참여자 (새로운 참여자)
        val participantsToAdd = newParticipants - this.participants

        // 제거할 참여자 (더 이상 참여하지 않는 사용자)
        val participantsToRemove = this.participants - newParticipants

        // 핀 상태가 변경된 참여자
        val pinnedStatusChanges = mutableMapOf<UserId, Boolean>()

        // 새 참여자 중 핀 상태 확인
        (this.participants intersect newParticipants).forEach { participantId ->
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
         * @param validator 사용자 유효성 검사 함수
         * @return 새로운 1:1 채팅방
         * @throws IllegalArgumentException 유효하지 않은 사용자인 경우
         */
        fun createDirectChat(
            userId: Long,
            friendId: Long,
            friendName: String,
            validator: (UserId, UserId) -> Unit = { _, _ -> }
        ): ChatRoom {
            val userIdVo = UserId.from(userId)
            val friendIdVo = UserId.from(friendId)
            
            // 비즈니스 규칙 검증
            if (userId == friendId) {
                throw IllegalArgumentException("자기 자신과는 채팅방을 만들 수 없습니다.")
            }
            
            // 외부 검증 로직 실행 (사용자 존재 여부 등)
            validator(userIdVo, friendIdVo)
            
            val title = ChatRoomTitle.from("${friendName}님과의 대화")

            return ChatRoom(
                title = title,
                type = ChatRoomType.INDIVIDUAL,
                announcement = null,
                participants = setOf(userIdVo, friendIdVo),
                pinnedParticipants = emptySet(),
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
        title: ChatRoomTitle? = null,
        announcement: ChatRoomAnnouncement? = null,
        lastMessageId: MessageId? = null,
        lastActiveAt: Instant? = null
    ) {
        title?.let { this.title = it }
        announcement?.let { this.announcement = it }
        lastMessageId?.let { this.lastMessageId = it }
        lastActiveAt?.let { this.lastActiveAt = it }
        this.updatedAt = Instant.now()
    }

    /**
     * 참여자 추가
     *
     * @param userId 추가할 사용자 ID
     * @return 추가 성공 여부
     */
    fun addParticipant(userId: UserId): Boolean {
        // 이미 참여 중인지 확인
        if (participants.contains(userId)) {
            return false
        }

        this.participants = participants + userId
        this.updatedAt = Instant.now()
        return true
    }

    /**
     * 참여자 제거
     *
     * @param userId 제거할 사용자 ID
     * @return 제거 성공 여부
     */
    fun removeParticipant(userId: UserId): Boolean {
        // 참여자가 아닌 경우
        if (!participants.contains(userId)) {
            return false
        }

        this.participants = participants - userId
        this.updatedAt = Instant.now()
        return true
    }

    /**
     * 여러 참여자 추가
     */
    fun addParticipants(userIds: Collection<UserId>) {
        this.participants = participants + userIds
        this.updatedAt = Instant.now()
    }

    /**
     * 여러 참여자 제거
     */
    fun removeParticipants(userIds: Collection<UserId>) {
        this.participants = participants - userIds.toSet()
        this.updatedAt = Instant.now()
    }

    /**
     * 참여자 목록 업데이트 (기존 참여자 유지하고 새 참여자 추가, 제외된 참여자 제거)
     */
    fun updateParticipants(newParticipants: Collection<UserId>) {
        val newParticipantsSet = newParticipants.toSet()
        val participantsToAdd = newParticipantsSet - this.participants
        val participantsToRemove = this.participants - newParticipantsSet

        if (participantsToAdd.isNotEmpty()) {
            this.addParticipants(participantsToAdd)
        }

        if (participantsToRemove.isNotEmpty()) {
            this.removeParticipants(participantsToRemove)
        }
    }

    /**
     * 즐겨찾기(핀) 상태 업데이트
     *
     * @param userId 사용자 ID
     * @param isFavorite 즐겨찾기 여부
     * @param userPinnedRoomsCount 사용자가 현재 핀한 채팅방 수
     */
    fun updateFavoriteStatus(
        userId: UserId,
        isFavorite: Boolean,
        userPinnedRoomsCount: Int
    ) {
        this.pinnedParticipants = updatePinnedParticipants(userId, isFavorite, userPinnedRoomsCount)
        this.updatedAt = Instant.now()
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
        userId: UserId,
        isFavorite: Boolean,
        userPinnedRoomsCount: Int
    ): Set<UserId> {
        val isAlreadyPinned = pinnedParticipants.contains(userId)

        return when {
            // 이미 즐겨찾기된 채팅방을 다시 즐겨찾기하려고 하면 제거 (토글 동작)
            isFavorite && isAlreadyPinned -> pinnedParticipants - userId
            // 즐겨찾기 추가 요청이고 아직 즐겨찾기되지 않은 경우
            isFavorite -> {
                if (userPinnedRoomsCount >= MAX_PINNED) {
                    throw FavoriteLimitExceededException("최대 핀 채팅방 개수를 초과했습니다. (MAX_PINNED=$MAX_PINNED)")
                }
                pinnedParticipants + userId
            }
            // 즐겨찾기 해제 요청
            else -> pinnedParticipants - userId
        }
    }

    /**
     * 채팅방 공지사항 업데이트
     *
     * @param announcement 새 공지사항 (null인 경우 공지사항 삭제)
     */
    fun updateAnnouncement(announcement: ChatRoomAnnouncement?) {
        this.announcement = announcement
        this.updatedAt = Instant.now()
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
    fun isDirectChatBetween(
        userId1: UserId,
        userId2: UserId
    ): Boolean {
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
    fun createChatRoomTitle(userId: UserId): String {
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