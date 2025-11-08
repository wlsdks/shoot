package com.stark.shoot.adapter.`in`.rest.user

import com.stark.shoot.adapter.`in`.rest.dto.user.UpdateStatusRequest
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.application.port.`in`.user.profile.UserStatusUseCase
import com.stark.shoot.application.port.`in`.user.profile.command.UpdateUserStatusCommand
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.domain.user.vo.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.core.Authentication
import java.time.Instant

@DisplayName("UserStatusController 단위 테스트")
class UserStatusControllerTest {

    private val userStatusUseCase = mock(UserStatusUseCase::class.java)
    private val authentication = mock(Authentication::class.java)
    private val controller = UserStatusController(userStatusUseCase)

    @Test
    @DisplayName("[happy] 사용자 상태를 변경한다")
    fun `사용자 상태를 변경한다`() {
        // given
        val userId = 1L
        val request = UpdateStatusRequest(userId.toString(), UserStatus.AWAY)
        val command = UpdateUserStatusCommand.of(authentication, UserStatus.AWAY)

        val updatedUser = createUser(
            id = userId,
            username = "testuser",
            nickname = "테스트 유저",
            status = UserStatus.AWAY
        )

        `when`(userStatusUseCase.updateStatus(command))
            .thenReturn(updatedUser)

        // when
        val response = controller.updateStatus(authentication, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(userId.toString())
        assertThat(response.data?.status).isEqualTo(UserStatus.AWAY)
        assertThat(response.message).isEqualTo("사용자 상태가 변경되었습니다.")

        verify(userStatusUseCase).updateStatus(command)
    }

    // 테스트용 User 객체 생성 헬퍼 메서드
    private fun createUser(
        id: Long,
        username: String,
        nickname: String,
        status: UserStatus = UserStatus.ONLINE,
        profileImageUrl: String? = null,
        backgroundImageUrl: String? = null,
        bio: String? = null
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
