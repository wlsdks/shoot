package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.user.*
import com.stark.shoot.application.port.`in`.user.FindUserUseCase
import com.stark.shoot.application.port.`in`.user.command.FindUserByIdCommand
import com.stark.shoot.application.port.`in`.user.profile.UserUpdateProfileUseCase
import com.stark.shoot.application.port.`in`.user.profile.command.SetBackgroundImageCommand
import com.stark.shoot.application.port.`in`.user.profile.command.SetProfileImageCommand
import com.stark.shoot.application.port.`in`.user.profile.command.UpdateProfileCommand
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "사용자", description = "사용자 관련 API")
@RequestMapping("/api/v1/users")
@RestController
class UserProfileController(
    private val userUpdateProfileUseCase: UserUpdateProfileUseCase,
    private val findUserUseCase: FindUserUseCase
) {

    @Operation(
        summary = "프로필 수정",
        description = "사용자의 프로필 정보를 수정합니다."
    )
    @PutMapping("/me")
    fun updateProfile(
        authentication: Authentication,
        @RequestBody request: UpdateProfileRequest
    ): ResponseDto<UserResponse> {
        val userId = authentication.name.toLong()
        val command = UpdateProfileCommand.of(
            userId = userId,
            nickname = request.nickname,
            profileImageUrl = request.profileImageUrl,
            backgroundImageUrl = request.backgroundImageUrl,
            bio = request.bio
        )
        val user = userUpdateProfileUseCase.updateProfile(command)
        return ResponseDto.success(user.toResponse(), "프로필이 성공적으로 업데이트되었습니다.")
    }

    @Operation(
        summary = "프로필 사진 설정하기",
        description = "사용자의 프로필 사진을 설정합니다."
    )
    @PutMapping("/me/profile-image")
    fun setProfileImage(
        authentication: Authentication,
        @RequestBody request: SetProfileImageRequest
    ): ResponseDto<UserResponse> {
        val userId = authentication.name.toLong()
        val command = SetProfileImageCommand.of(
            userId = userId,
            profileImageUrl = request.profileImageUrl
        )
        val user = userUpdateProfileUseCase.setProfileImage(command)
        return ResponseDto.success(user.toResponse(), "프로필 사진이 성공적으로 설정되었습니다.")
    }

    @Operation(
        summary = "내 배경 설정하기",
        description = "사용자의 배경 이미지를 설정합니다."
    )
    @PutMapping("/me/background-image")
    fun setBackgroundImage(
        authentication: Authentication,
        @RequestBody request: SetBackgroundImageRequest
    ): ResponseDto<UserResponse> {
        val userId = authentication.name.toLong()
        val command = SetBackgroundImageCommand.of(
            userId = userId,
            backgroundImageUrl = request.backgroundImageUrl
        )
        val user = userUpdateProfileUseCase.setBackgroundImage(command)
        return ResponseDto.success(user.toResponse(), "배경 이미지가 성공적으로 설정되었습니다.")
    }

    @Operation(
        summary = "친구 프로필 조회",
        description = "친구의 프로필 정보를 조회합니다."
    )
    @GetMapping("/{userId}")
    fun getUserProfile(
        @PathVariable userId: Long
    ): ResponseDto<UserResponse> {
        val command = FindUserByIdCommand.of(userId)
        val user = findUserUseCase.findById(command)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")
        return ResponseDto.success(user.toResponse(), "프로필 정보를 성공적으로 조회했습니다.")
    }

}
