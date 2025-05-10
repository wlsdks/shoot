package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.user.SetBackgroundImageRequest
import com.stark.shoot.adapter.`in`.web.dto.user.SetProfileImageRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UpdateProfileRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.profile.UserUpdateProfileUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "사용자", description = "사용자 관련 API")
@RequestMapping("/api/v1/users")
@RestController
class UserProfileController(
    private val userUpdateProfileUseCase: UserUpdateProfileUseCase
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
        val user = userUpdateProfileUseCase.updateProfile(userId, request)
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
        val user = userUpdateProfileUseCase.setProfileImage(userId, request)
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
        val user = userUpdateProfileUseCase.setBackgroundImage(userId, request)
        return ResponseDto.success(user.toResponse(), "배경 이미지가 성공적으로 설정되었습니다.")
    }
}
