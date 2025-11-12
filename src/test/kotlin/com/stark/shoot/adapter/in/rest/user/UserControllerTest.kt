package com.stark.shoot.adapter.`in`.rest.user

import com.stark.shoot.adapter.`in`.rest.dto.user.CreateUserRequest
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import com.stark.shoot.application.port.`in`.user.UserDeleteUseCase
import com.stark.shoot.application.port.`in`.user.command.CreateUserCommand
import com.stark.shoot.application.port.`in`.user.command.DeleteUserCommand
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.domain.user.vo.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.Authentication
import java.time.Instant

@DisplayName("UserController 단위 테스트")
class UserControllerTest {

    private val userCreateUseCase = mock(UserCreateUseCase::class.java)
    private val userDeleteUseCase = mock(UserDeleteUseCase::class.java)
    private val controller = UserController(userCreateUseCase, userDeleteUseCase)

    @Test
    @DisplayName("[happy] 사용자 생성 요청을 처리하고 생성된 사용자를 반환한다")
    fun `사용자 생성 요청을 처리하고 생성된 사용자를 반환한다`() {
        // given
        val username = "testuser"
        val nickname = "Test User"
        val password = "Password123!"
        val email = "test@example.com"
        val bio = "This is a test user"

        val profileImage = MockMultipartFile(
            "profileImage",
            "test-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".toByteArray()
        )

        val request = CreateUserRequest(
            username = username,
            nickname = nickname,
            password = password,
            email = email,
            bio = bio,
            profileImage = profileImage
        )

        val command = CreateUserCommand.of(request)

        val user = User(
            id = UserId.from(1L),
            username = Username.from(username),
            nickname = Nickname.from(nickname),
            status = UserStatus.OFFLINE,
            passwordHash = "hashed_password",
            userCode = UserCode.from("12345678"),
            profileImageUrl = ProfileImageUrl.from("http://example.com/image.jpg"),
            bio = UserBio.from(bio),
            createdAt = Instant.now()
        )

        `when`(userCreateUseCase.createUser(command)).thenReturn(user)

        // when
        val response = controller.createUser(request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.username).isEqualTo(username)
        assertThat(response.data?.nickname).isEqualTo(nickname)
        assertThat(response.data?.bio).isEqualTo(bio)
        assertThat(response.message).isEqualTo("회원가입이 완료되었습니다.")

        verify(userCreateUseCase).createUser(command)
    }

    @Test
    @DisplayName("[happy] 회원 탈퇴 요청을 처리하고 성공 메시지를 반환한다")
    fun `회원 탈퇴 요청을 처리하고 성공 메시지를 반환한다`() {
        // given
        val userId = 1L
        val authentication = mock(Authentication::class.java)
        `when`(authentication.name).thenReturn(userId.toString())

        val command = DeleteUserCommand.of(userId)
        doNothing().`when`(userDeleteUseCase).deleteUser(command)

        // when
        val response = controller.deleteUser(authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("회원 탈퇴가 완료되었습니다.")

        verify(userDeleteUseCase).deleteUser(command)
    }
}
