package com.stark.shoot.domain.user

import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.domain.user.vo.*
import com.stark.shoot.domain.user.exception.InvalidUserDataException
import com.stark.shoot.infrastructure.annotation.AggregateRoot
import java.time.Instant

@AggregateRoot
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
         * 보안 강화를 위해 다음 요구사항을 만족해야 합니다:
         * - 최소 8자 이상
         * - 최소 1개의 대문자 포함
         * - 최소 1개의 소문자 포함
         * - 최소 1개의 숫자 포함
         * - 최소 1개의 특수문자 포함
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
            if (!password.any { it.isUpperCase() }) {
                throw InvalidUserDataException("비밀번호는 최소 1개의 대문자를 포함해야 합니다.")
            }
            if (!password.any { it.isLowerCase() }) {
                throw InvalidUserDataException("비밀번호는 최소 1개의 소문자를 포함해야 합니다.")
            }
            if (!password.any { it.isDigit() }) {
                throw InvalidUserDataException("비밀번호는 최소 1개의 숫자를 포함해야 합니다.")
            }
            if (!password.any { !it.isLetterOrDigit() }) {
                throw InvalidUserDataException("비밀번호는 최소 1개의 특수문자를 포함해야 합니다.")
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
     * 사용자 코드를 특정 값으로 변경
     *
     * @param newCode 새로운 유저 코드
     * @param codeValidator 코드 중복 검사 함수
     * @throws IllegalArgumentException 중복된 코드인 경우
     */
    fun changeUserCode(
        newCode: UserCode,
        codeValidator: (UserCode) -> Boolean
    ) {
        if (!codeValidator(newCode)) {
            throw InvalidUserDataException("이미 사용 중인 유저 코드입니다: ${newCode.value}")
        }

        this.userCode = newCode
        this.updatedAt = Instant.now()
    }

    /**
     * 사용자 계정 삭제 (소프트 삭제)
     */
    fun markAsDeleted() {
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