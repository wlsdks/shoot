package com.stark.shoot.domain.user

import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.domain.user.vo.*
import com.stark.shoot.infrastructure.exception.InvalidUserDataException
import java.time.Instant

data class User(
    val id: UserId? = null,
    val username: Username,
    var nickname: Nickname,
    var status: UserStatus = UserStatus.OFFLINE,
    var passwordHash: String? = null,
    var userCode: UserCode,
    val createdAt: Instant = Instant.now(),

    // 필요한 경우에만 남길 선택적 필드
    var profileImageUrl: ProfileImageUrl? = null,
    var backgroundImageUrl: BackgroundImageUrl? = null,
    var lastSeenAt: Instant? = null,
    var bio: UserBio? = null,
    var isDeleted: Boolean = false,
    var updatedAt: Instant? = null,
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
            // 유효성 검증 및 값 객체 생성
            val usernameVo = Username.from(username)
            val nicknameVo = Nickname.from(nickname)
            validatePassword(rawPassword)

            return User(
                username = usernameVo,
                nickname = nicknameVo,
                passwordHash = passwordEncoder(rawPassword),
                userCode = UserCode.generate(),
                bio = bio?.let { UserBio.from(it) },
                profileImageUrl = profileImageUrl?.let { ProfileImageUrl.from(it) }
            )
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
     * 사용자 코드 재생성
     * 
     * @return 새로 생성된 사용자 코드
     */
    fun generateUserCode(): UserCode {
        this.userCode = UserCode.generate()
        this.updatedAt = Instant.now()
        return this.userCode
    }

    /**
     * 사용자 계정 삭제 (소프트 삭제)
     */
    fun delete() {
        this.isDeleted = true
        this.status = UserStatus.OFFLINE
        this.updatedAt = Instant.now()
    }

    /**
     * 프로필 정보 업데이트
     *
     * @param nickname 새 닉네임 (null인 경우 기존 값 유지)
     * @param bio 새 자기소개 (null인 경우 기존 값 유지)  
     * @param profileImageUrl 새 프로필 이미지 URL (null인 경우 기존 값 유지)
     * @param backgroundImageUrl 새 배경 이미지 URL (null인 경우 기존 값 유지)
     * @throws InvalidUserDataException 유효하지 않은 데이터가 제공된 경우
     */
    fun updateProfile(
        nickname: String? = null,
        bio: String? = null,
        profileImageUrl: String? = null,
        backgroundImageUrl: String? = null
    ) {
        // 유효성 검증 및 업데이트
        nickname?.let { this.nickname = Nickname.from(it) }
        bio?.let { this.bio = UserBio.from(it) }
        profileImageUrl?.let { this.profileImageUrl = ProfileImageUrl.from(it) }
        backgroundImageUrl?.let { this.backgroundImageUrl = BackgroundImageUrl.from(it) }
        
        this.updatedAt = Instant.now()
    }

    /**
     * 프로필 이미지 변경
     *
     * @param imageUrl 새 프로필 이미지 URL
     */
    fun changeProfileImage(imageUrl: String) {
        this.profileImageUrl = ProfileImageUrl.from(imageUrl)
        this.updatedAt = Instant.now()
    }

    /**
     * 배경 이미지 변경
     *
     * @param imageUrl 새 배경 이미지 URL
     */
    fun changeBackgroundImage(imageUrl: String) {
        this.backgroundImageUrl = BackgroundImageUrl.from(imageUrl)
        this.updatedAt = Instant.now()
    }

}