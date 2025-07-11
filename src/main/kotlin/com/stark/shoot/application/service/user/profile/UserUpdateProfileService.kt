package com.stark.shoot.application.service.user.profile

import com.stark.shoot.application.port.`in`.user.profile.UserUpdateProfileUseCase
import com.stark.shoot.application.port.`in`.user.profile.command.SetBackgroundImageCommand
import com.stark.shoot.application.port.`in`.user.profile.command.SetProfileImageCommand
import com.stark.shoot.application.port.`in`.user.profile.command.UpdateProfileCommand
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UserUpdateProfileService(
    private val userQueryPort: UserQueryPort,
    private val userCommandPort: UserCommandPort
) : UserUpdateProfileUseCase {

    /**
     * 프로필 수정
     *
     * @param command 프로필 수정 커맨드
     * @return 수정된 사용자 정보
     */
    override fun updateProfile(command: UpdateProfileCommand): User {
        // 사용자 정보 조회
        val user = userQueryPort.findUserById(command.userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${command.userId}")

        // 도메인 객체의 메서드를 직접 호출하여 프로필 업데이트
        val updatedUser = user.updateProfile(
            nickname = command.nickname?.value,
            bio = command.bio?.value,
            profileImageUrl = command.profileImageUrl?.value,
            backgroundImageUrl = command.backgroundImageUrl?.value
        )

        return userCommandPort.updateUser(updatedUser)
    }

    /**
     * 프로필 사진 설정
     *
     * @param command 프로필 사진 설정 커맨드
     * @return 수정된 사용자 정보
     */
    override fun setProfileImage(command: SetProfileImageCommand): User {
        // 사용자 정보 조회
        val user = userQueryPort.findUserById(command.userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${command.userId}")

        // 도메인 객체의 메서드를 직접 호출하여 프로필 이미지 변경
        val updatedUser = user.changeProfileImage(
            imageUrl = command.profileImageUrl.value
        )

        return userCommandPort.updateUser(updatedUser)
    }

    /**
     * 배경 이미지 설정
     *
     * @param command 배경 이미지 설정 커맨드
     * @return 수정된 사용자 정보
     */
    override fun setBackgroundImage(command: SetBackgroundImageCommand): User {
        // 사용자 정보 조회
        val user = userQueryPort.findUserById(command.userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${command.userId}")

        // 도메인 객체의 메서드를 직접 호출하여 배경 이미지 변경
        val updatedUser = user.changeBackgroundImage(
            imageUrl = command.backgroundImageUrl.value
        )

        return userCommandPort.updateUser(updatedUser)
    }

}
