package com.stark.shoot.application.service.user.profile

import com.stark.shoot.adapter.`in`.web.dto.user.SetBackgroundImageRequest
import com.stark.shoot.adapter.`in`.web.dto.user.SetProfileImageRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UpdateProfileRequest
import com.stark.shoot.application.port.`in`.user.profile.UserUpdateProfileUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UserUpdateProfileService(
    private val findUserPort: FindUserPort,
    private val userUpdatePort: UserUpdatePort
) : UserUpdateProfileUseCase {

    /**
     * 프로필 수정
     *
     * @param userId 사용자 ID
     * @param request 프로필 수정 요청
     * @return 수정된 사용자 정보
     */
    override fun updateProfile(
        userId: UserId,
        request: UpdateProfileRequest
    ): User {
        // 사용자 정보 조회
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        // 도메인 객체의 메서드를 직접 호출하여 프로필 업데이트
        val updatedUser = user.updateProfile(
            nickname = request.nickname,
            bio = request.bio,
            profileImageUrl = request.profileImageUrl,
            backgroundImageUrl = request.backgroundImageUrl
        )

        return userUpdatePort.updateUser(updatedUser)
    }

    /**
     * 프로필 사진 설정
     *
     * @param userId 사용자 ID
     * @param request 프로필 사진 설정 요청
     * @return 수정된 사용자 정보
     */
    override fun setProfileImage(
        userId: UserId,
        request: SetProfileImageRequest
    ): User {
        // 사용자 정보 조회
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        // 도메인 객체의 메서드를 직접 호출하여 프로필 이미지 변경
        val updatedUser = user.changeProfileImage(
            imageUrl = request.profileImageUrl
        )

        return userUpdatePort.updateUser(updatedUser)
    }

    /**
     * 배경 이미지 설정
     *
     * @param userId 사용자 ID
     * @param request 배경 이미지 설정 요청
     * @return 수정된 사용자 정보
     */
    override fun setBackgroundImage(
        userId: UserId,
        request: SetBackgroundImageRequest
    ): User {
        // 사용자 정보 조회
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        // 도메인 객체의 메서드를 직접 호출하여 배경 이미지 변경
        val updatedUser = user.changeBackgroundImage(
            imageUrl = request.backgroundImageUrl
        )

        return userUpdatePort.updateUser(updatedUser)
    }

}
