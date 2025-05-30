package com.stark.shoot.domain.chat.user

import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.UserStatus
import com.stark.shoot.domain.exception.InvalidUserDataException
import java.time.Instant
import java.util.*

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
    var backgroundImageUrl: String? = null,
    var lastSeenAt: Instant? = null,
    var bio: String? = null,
    var isDeleted: Boolean = false,
    var updatedAt: Instant? = null,

    // 소셜 기능 관련 필드 (필요시 사용)
    var friendIds: Set<Long> = emptySet(),                 // 이미 친구인 사용자들의 id 목록
    var incomingFriendRequestIds: Set<Long> = emptySet(),  // 받은 친구 요청의 사용자 id 목록
    var outgoingFriendRequestIds: Set<Long> = emptySet(),  // 보낸 친구 요청의 사용자 id 목록
) {

    companion object {
        /**
         * 사용자 생성 팩토리 메서드
         *
         * @param username 사용자명
         * @param nickname 닉네임
         * @param rawPassword 암호화되지 않은 비밀번호
         * @param passwordEncoder 비밀번호 암호화 함수
         * @param bio 자기소개 (선택)
         * @param profileImageUrl 프로필 이미지 URL (선택)
         * @return 생성된 User 객체
         * @throws InvalidUserDataException 유효하지 않은 사용자 데이터
         */
        fun create(
            username: String,
            nickname: String,
            rawPassword: String,
            passwordEncoder: (String) -> String,
            bio: String? = null,
            profileImageUrl: String? = null
        ): User {
            // 유효성 검증
            validateUsername(username)
            validateNickname(nickname)
            validatePassword(rawPassword)

            return User(
                username = username,
                nickname = nickname,
                passwordHash = passwordEncoder(rawPassword),
                userCode = generateUserCode(),
                bio = bio,
                profileImageUrl = profileImageUrl
            )
        }

        /**
         * 사용자 코드 생성
         *
         * @return 생성된 8자리 사용자 코드
         */
        private fun generateUserCode(): String {
            return UUID.randomUUID().toString().substring(0, 8).uppercase()
        }

        /**
         * 사용자명 유효성 검증
         *
         * @param username 검증할 사용자명
         * @throws InvalidUserDataException 유효하지 않은 사용자명
         */
        private fun validateUsername(username: String) {
            if (username.isBlank()) {
                throw InvalidUserDataException("사용자명은 비어있을 수 없습니다.")
            }
            if (username.length < 3 || username.length > 20) {
                throw InvalidUserDataException("사용자명은 3-20자 사이여야 합니다.")
            }
        }

        /**
         * 닉네임 유효성 검증
         *
         * @param nickname 검증할 닉네임
         * @throws InvalidUserDataException 유효하지 않은 닉네임
         */
        private fun validateNickname(nickname: String) {
            if (nickname.isBlank()) {
                throw InvalidUserDataException("닉네임은 비어있을 수 없습니다.")
            }
            if (nickname.length < 2 || nickname.length > 30) {
                throw InvalidUserDataException("닉네임은 2-30자 사이여야 합니다.")
            }
        }

        /**
         * 비밀번호 유효성 검증
         *
         * @param password 검증할 비밀번호
         * @throws InvalidUserDataException 유효하지 않은 비밀번호
         */
        private fun validatePassword(password: String) {
            if (password.isBlank()) {
                throw InvalidUserDataException("비밀번호는 비어있을 수 없습니다.")
            }
            if (password.length < 8) {
                throw InvalidUserDataException("비밀번호는 최소 8자 이상이어야 합니다.")
            }
        }
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

    /**
     * 친구 제거
     *
     * @param friendId 제거할 친구 ID
     * @return 업데이트된 User 객체
     */
    fun removeFriend(friendId: Long): User {
        // 친구 목록에서 제거
        val updatedFriendIds = this.friendIds.toMutableSet()
        updatedFriendIds.remove(friendId)

        return this.copy(
            friendIds = updatedFriendIds,
            updatedAt = Instant.now()
        )
    }

    /**
     * 프로필 정보 업데이트
     *
     * @param nickname 새 닉네임 (null인 경우 기존 값 유지)
     * @param bio 새 자기소개 (null인 경우 기존 값 유지)
     * @param profileImageUrl 새 프로필 이미지 URL (null인 경우 기존 값 유지)
     * @param backgroundImageUrl 새 배경 이미지 URL (null인 경우 기존 값 유지)
     * @return 업데이트된 User 객체
     * @throws InvalidUserDataException 유효하지 않은 데이터가 제공된 경우
     */
    fun updateProfile(
        nickname: String? = null,
        bio: String? = null,
        profileImageUrl: String? = null,
        backgroundImageUrl: String? = null
    ): User {
        // 닉네임 유효성 검증
        if (nickname != null) {
            validateNickname(nickname)
        }

        // 업데이트된 사용자 정보 반환
        return this.copy(
            nickname = nickname ?: this.nickname,
            bio = bio ?: this.bio,
            profileImageUrl = profileImageUrl ?: this.profileImageUrl,
            backgroundImageUrl = backgroundImageUrl ?: this.backgroundImageUrl,
            updatedAt = Instant.now()
        )
    }

    /**
     * 프로필 이미지 변경
     *
     * @param imageUrl 새 프로필 이미지 URL
     * @return 업데이트된 User 객체
     */
    fun changeProfileImage(imageUrl: String): User {
        return this.copy(
            profileImageUrl = imageUrl,
            updatedAt = Instant.now()
        )
    }

    /**
     * 배경 이미지 변경
     *
     * @param imageUrl 새 배경 이미지 URL
     * @return 업데이트된 User 객체
     */
    fun changeBackgroundImage(imageUrl: String): User {
        return this.copy(
            backgroundImageUrl = imageUrl,
            updatedAt = Instant.now()
        )
    }

}
