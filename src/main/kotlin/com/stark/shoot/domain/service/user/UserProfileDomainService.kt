package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.domain.exception.InvalidUserDataException
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * 사용자 프로필 관련 도메인 서비스
 * 프로필 업데이트, 이미지 설정 등의 도메인 로직을 담당합니다.
 */
@Service
class UserProfileDomainService {

    /**
     * 사용자 프로필을 업데이트합니다.
     *
     * @param user 현재 사용자 정보
     * @param nickname 새 닉네임 (null인 경우 기존 값 유지)
     * @param bio 새 자기소개 (null인 경우 기존 값 유지)
     * @param profileImageUrl 새 프로필 이미지 URL (null인 경우 기존 값 유지)
     * @param backgroundImageUrl 새 배경 이미지 URL (null인 경우 기존 값 유지)
     * @return 업데이트된 사용자 정보
     * @throws InvalidUserDataException 유효하지 않은 데이터가 제공된 경우
     */
    fun updateProfile(
        user: User,
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
        return user.copy(
            nickname = nickname ?: user.nickname,
            bio = bio ?: user.bio,
            profileImageUrl = profileImageUrl ?: user.profileImageUrl,
            backgroundImageUrl = backgroundImageUrl ?: user.backgroundImageUrl,
            updatedAt = Instant.now()
        )
    }

    /**
     * 프로필 이미지를 설정합니다.
     *
     * @param user 현재 사용자 정보
     * @param profileImageUrl 새 프로필 이미지 URL
     * @return 업데이트된 사용자 정보
     */
    fun setProfileImage(
        user: User,
        profileImageUrl: String
    ): User {
        // URL 유효성 검증 (필요시 구현)
        
        // 업데이트된 사용자 정보 반환
        return user.copy(
            profileImageUrl = profileImageUrl,
            updatedAt = Instant.now()
        )
    }

    /**
     * 배경 이미지를 설정합니다.
     *
     * @param user 현재 사용자 정보
     * @param backgroundImageUrl 새 배경 이미지 URL
     * @return 업데이트된 사용자 정보
     */
    fun setBackgroundImage(
        user: User,
        backgroundImageUrl: String
    ): User {
        // URL 유효성 검증 (필요시 구현)
        
        // 업데이트된 사용자 정보 반환
        return user.copy(
            backgroundImageUrl = backgroundImageUrl,
            updatedAt = Instant.now()
        )
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
}