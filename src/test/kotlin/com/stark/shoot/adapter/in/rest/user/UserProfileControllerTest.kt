package com.stark.shoot.adapter.`in`.rest.user

import com.stark.shoot.adapter.`in`.rest.dto.user.SetBackgroundImageRequest
import com.stark.shoot.adapter.`in`.rest.dto.user.SetProfileImageRequest
import com.stark.shoot.adapter.`in`.rest.dto.user.UpdateProfileRequest
import com.stark.shoot.application.port.`in`.user.FindUserUseCase
import com.stark.shoot.application.port.`in`.user.command.FindUserByIdCommand
import com.stark.shoot.application.port.`in`.user.profile.UserUpdateProfileUseCase
import com.stark.shoot.application.port.`in`.user.profile.command.SetBackgroundImageCommand
import com.stark.shoot.application.port.`in`.user.profile.command.SetProfileImageCommand
import com.stark.shoot.application.port.`in`.user.profile.command.UpdateProfileCommand
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.domain.user.vo.*
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.security.core.Authentication
import java.time.Instant

@DisplayName("UserProfileController 단위 테스트")
class UserProfileControllerTest {

    private val userUpdateProfileUseCase = mock(UserUpdateProfileUseCase::class.java)
    private val findUserUseCase = mock(FindUserUseCase::class.java)
    private val authentication = mock(Authentication::class.java)
    private val controller = UserProfileController(userUpdateProfileUseCase, findUserUseCase)

    @Test
    @DisplayName("[happy] 사용자 프로필을 업데이트한다")
    fun `사용자 프로필을 업데이트한다`() {
        // given
        val userId = 1L
        val request = UpdateProfileRequest(
            nickname = "새로운 닉네임",
            profileImageUrl = "http://example.com/new-profile.jpg",
            backgroundImageUrl = "http://example.com/new-background.jpg",
            bio = "새로운 자기소개"
        )

        val command = UpdateProfileCommand.of(
            userId = userId,
            nickname = "새로운 닉네임",
            profileImageUrl = "http://example.com/new-profile.jpg",
            backgroundImageUrl = "http://example.com/new-background.jpg",
            bio = "새로운 자기소개"
        )

        val updatedUser = createUser(
            id = userId,
            username = "testuser",
            nickname = "새로운 닉네임",
            profileImageUrl = "http://example.com/new-profile.jpg",
            backgroundImageUrl = "http://example.com/new-background.jpg",
            bio = "새로운 자기소개"
        )

        `when`(authentication.name).thenReturn(userId.toString())
        `when`(userUpdateProfileUseCase.updateProfile(command))
            .thenReturn(updatedUser)

        // when
        val response = controller.updateProfile(authentication, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(userId.toString())
        assertThat(response.data?.nickname).isEqualTo("새로운 닉네임")
        assertThat(response.data?.profileImageUrl).isEqualTo("http://example.com/new-profile.jpg")
        assertThat(response.data?.backgroundImageUrl).isEqualTo("http://example.com/new-background.jpg")
        assertThat(response.data?.bio).isEqualTo("새로운 자기소개")
        assertThat(response.message).isEqualTo("프로필이 성공적으로 업데이트되었습니다.")

        verify(authentication).name
        verify(userUpdateProfileUseCase).updateProfile(command)
    }

    @Test
    @DisplayName("[happy] 사용자 프로필 이미지를 설정한다")
    fun `사용자 프로필 이미지를 설정한다`() {
        // given
        val userId = 1L
        val request = SetProfileImageRequest("http://example.com/profile.jpg")

        val command = SetProfileImageCommand.of(
            userId = userId,
            profileImageUrl = "http://example.com/profile.jpg"
        )

        val updatedUser = createUser(
            id = userId,
            username = "testuser",
            nickname = "테스트 유저",
            profileImageUrl = "http://example.com/profile.jpg"
        )

        `when`(authentication.name).thenReturn(userId.toString())
        `when`(userUpdateProfileUseCase.setProfileImage(command))
            .thenReturn(updatedUser)

        // when
        val response = controller.setProfileImage(authentication, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(userId.toString())
        assertThat(response.data?.profileImageUrl).isEqualTo("http://example.com/profile.jpg")
        assertThat(response.message).isEqualTo("프로필 사진이 성공적으로 설정되었습니다.")

        verify(authentication).name
        verify(userUpdateProfileUseCase).setProfileImage(command)
    }

    @Test
    @DisplayName("[happy] 사용자 배경 이미지를 설정한다")
    fun `사용자 배경 이미지를 설정한다`() {
        // given
        val userId = 1L
        val request = SetBackgroundImageRequest("http://example.com/background.jpg")

        val command = SetBackgroundImageCommand.of(
            userId = userId,
            backgroundImageUrl = "http://example.com/background.jpg"
        )

        val updatedUser = createUser(
            id = userId,
            username = "testuser",
            nickname = "테스트 유저",
            backgroundImageUrl = "http://example.com/background.jpg"
        )

        `when`(authentication.name).thenReturn(userId.toString())
        `when`(userUpdateProfileUseCase.setBackgroundImage(command))
            .thenReturn(updatedUser)

        // when
        val response = controller.setBackgroundImage(authentication, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(userId.toString())
        assertThat(response.data?.backgroundImageUrl).isEqualTo("http://example.com/background.jpg")
        assertThat(response.message).isEqualTo("배경 이미지가 성공적으로 설정되었습니다.")

        verify(authentication).name
        verify(userUpdateProfileUseCase).setBackgroundImage(command)
    }

    @Test
    @DisplayName("[happy] 사용자 프로필을 조회한다")
    fun `사용자 프로필을 조회한다`() {
        // given
        val userId = 1L
        val command = FindUserByIdCommand.of(userId)

        val user = createUser(
            id = userId,
            username = "testuser",
            nickname = "테스트 유저",
            profileImageUrl = "http://example.com/profile.jpg",
            backgroundImageUrl = "http://example.com/background.jpg",
            bio = "자기소개"
        )

        `when`(findUserUseCase.findById(command))
            .thenReturn(user)

        // when
        val response = controller.getUserProfile(userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(userId.toString())
        assertThat(response.data?.username).isEqualTo("testuser")
        assertThat(response.data?.nickname).isEqualTo("테스트 유저")
        assertThat(response.data?.profileImageUrl).isEqualTo("http://example.com/profile.jpg")
        assertThat(response.data?.backgroundImageUrl).isEqualTo("http://example.com/background.jpg")
        assertThat(response.data?.bio).isEqualTo("자기소개")
        assertThat(response.message).isEqualTo("프로필 정보를 성공적으로 조회했습니다.")

        verify(findUserUseCase).findById(command)
    }

    @Test
    @DisplayName("[fail] 존재하지 않는 사용자 프로필 조회 시 예외가 발생한다")
    fun `존재하지 않는 사용자 프로필 조회 시 예외가 발생한다`() {
        // given
        val userId = 999L
        val command = FindUserByIdCommand.of(userId)

        `when`(findUserUseCase.findById(command))
            .thenReturn(null)

        // when & then
        assertThrows<ResourceNotFoundException> {
            controller.getUserProfile(userId)
        }

        verify(findUserUseCase).findById(command)
    }

    // 테스트용 User 객체 생성 헬퍼 메서드
    private fun createUser(
        id: Long,
        username: String,
        nickname: String,
        profileImageUrl: String? = null,
        backgroundImageUrl: String? = null,
        bio: String? = null,
        status: UserStatus = UserStatus.ONLINE
    ): User {
        return User(
            id = UserId.from(id),
            username = Username.from(username),
            nickname = Nickname.from(nickname),
            status = status,
            userCode = UserCode.from("USER123"),
            profileImageUrl = profileImageUrl?.let { ProfileImageUrl.from(it) },
            backgroundImageUrl = backgroundImageUrl?.let { BackgroundImageUrl.from(it) },
            bio = bio?.let { UserBio.from(it) },
            lastSeenAt = Instant.now()
        )
    }
}
