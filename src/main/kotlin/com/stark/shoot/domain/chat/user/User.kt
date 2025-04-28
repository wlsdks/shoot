package com.stark.shoot.domain.chat.user

import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.UserStatus
import java.time.Instant

data class User(
    val id: Long? = null,
    var username: String,
    var nickname: String,
    var status: UserStatus = UserStatus.OFFLINE,
    var passwordHash: String? = null,
    var userCode: String,
    val createdAt: Instant = Instant.now(),

    // 필요한 경우에만 남길 선택적 필드
    var profileImageUrl: String? = null,
    var lastSeenAt: Instant? = null,
    var bio: String? = null,
    var isDeleted: Boolean = false,
    var updatedAt: Instant? = null,

    // 소셜 기능 관련 필드 (필요시 사용)
    var friendIds: Set<Long> = emptySet(),                 // 이미 친구인 사용자들의 id 목록
    var incomingFriendRequestIds: Set<Long> = emptySet(),  // 받은 친구 요청의 사용자 id 목록
    var outgoingFriendRequestIds: Set<Long> = emptySet(),  // 보낸 친구 요청의 사용자 id 목록
) {
    /**
     * 사용자 코드 변경
     *
     * @param newCode 새 사용자 코드
     */
    fun changeUserCode(newCode: String) {
        this.userCode = newCode
    }

    /**
     * 사용자 프로필 정보 업데이트
     *
     * @param nickname 새 닉네임 (null이면 변경 안함)
     * @param profileImageUrl 새 프로필 이미지 URL (null이면 변경 안함)
     * @param bio 새 자기소개 (null이면 변경 안함)
     * @return 업데이트된 User 객체
     */
    fun updateProfile(
        nickname: String? = null,
        profileImageUrl: String? = null,
        bio: String? = null
    ): User {
        return this.copy(
            nickname = nickname ?: this.nickname,
            profileImageUrl = profileImageUrl ?: this.profileImageUrl,
            bio = bio ?: this.bio,
            updatedAt = Instant.now()
        )
    }

    /**
     * 사용자 상태 변경
     *
     * @param newStatus 새 상태
     * @return 업데이트된 User 객체
     */
    fun updateStatus(newStatus: UserStatus): User {
        return this.copy(
            status = newStatus,
            lastSeenAt = if (newStatus == UserStatus.OFFLINE) Instant.now() else this.lastSeenAt,
            updatedAt = Instant.now()
        )
    }

    /**
     * 친구 추가
     *
     * @param friendId 추가할 친구 ID
     * @return 업데이트된 User 객체
     */
    fun addFriend(friendId: Long): User {
        val updatedFriendIds = this.friendIds.toMutableSet()
        updatedFriendIds.add(friendId)

        val updatedOutgoingRequests = this.outgoingFriendRequestIds.toMutableSet()
        updatedOutgoingRequests.remove(friendId)

        val updatedIncomingRequests = this.incomingFriendRequestIds.toMutableSet()
        updatedIncomingRequests.remove(friendId)

        return this.copy(
            friendIds = updatedFriendIds,
            outgoingFriendRequestIds = updatedOutgoingRequests,
            incomingFriendRequestIds = updatedIncomingRequests,
            updatedAt = Instant.now()
        )
    }

    /**
     * 친구 제거
     *
     * @param friendId 제거할 친구 ID
     * @return 업데이트된 User 객체
     */
    fun removeFriend(friendId: Long): User {
        val updatedFriendIds = this.friendIds.toMutableSet()
        updatedFriendIds.remove(friendId)

        return this.copy(
            friendIds = updatedFriendIds,
            updatedAt = Instant.now()
        )
    }

    /**
     * 친구 요청 보내기
     *
     * @param userId 친구 요청을 보낼 사용자 ID
     * @return 업데이트된 User 객체
     */
    fun sendFriendRequest(userId: Long): User {
        // 이미 친구이거나 이미 요청을 보낸 경우 처리
        if (friendIds.contains(userId) || outgoingFriendRequestIds.contains(userId)) {
            return this
        }

        val updatedOutgoingRequests = this.outgoingFriendRequestIds.toMutableSet()
        updatedOutgoingRequests.add(userId)

        return this.copy(
            outgoingFriendRequestIds = updatedOutgoingRequests,
            updatedAt = Instant.now()
        )
    }

    /**
     * 받은 친구 요청 수락
     *
     * @param userId 수락할 친구 요청의 사용자 ID
     * @return 업데이트된 User 객체
     */
    fun acceptFriendRequest(userId: Long): User {
        // 요청이 없는 경우 처리
        if (!incomingFriendRequestIds.contains(userId)) {
            return this
        }

        val updatedFriendIds = this.friendIds.toMutableSet()
        updatedFriendIds.add(userId)

        val updatedIncomingRequests = this.incomingFriendRequestIds.toMutableSet()
        updatedIncomingRequests.remove(userId)

        return this.copy(
            friendIds = updatedFriendIds,
            incomingFriendRequestIds = updatedIncomingRequests,
            updatedAt = Instant.now()
        )
    }

    /**
     * 받은 친구 요청 거절
     *
     * @param userId 거절할 친구 요청의 사용자 ID
     * @return 업데이트된 User 객체
     */
    fun rejectFriendRequest(userId: Long): User {
        val updatedIncomingRequests = this.incomingFriendRequestIds.toMutableSet()
        updatedIncomingRequests.remove(userId)

        return this.copy(
            incomingFriendRequestIds = updatedIncomingRequests,
            updatedAt = Instant.now()
        )
    }

    /**
     * 보낸 친구 요청 취소
     *
     * @param userId 취소할 친구 요청의 사용자 ID
     * @return 업데이트된 User 객체
     */
    fun cancelFriendRequest(userId: Long): User {
        val updatedOutgoingRequests = this.outgoingFriendRequestIds.toMutableSet()
        updatedOutgoingRequests.remove(userId)

        return this.copy(
            outgoingFriendRequestIds = updatedOutgoingRequests,
            updatedAt = Instant.now()
        )
    }

    /**
     * 사용자 계정 삭제 (소프트 삭제)
     *
     * @return 삭제 표시된 User 객체
     */
    fun delete(): User {
        return this.copy(
            isDeleted = true,
            status = UserStatus.OFFLINE,
            updatedAt = Instant.now()
        )
    }
}
