package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.token.RefreshTokenUseCase
import com.stark.shoot.application.port.`in`.user.token.command.RefreshTokenCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

@DisplayName("TokenController 단위 테스트")
class TokenControllerTest {

    private val refreshTokenUseCase = mock(RefreshTokenUseCase::class.java)
    private val controller = TokenController(refreshTokenUseCase)

    @Test
    @DisplayName("[happy] 리프레시 토큰으로 새 액세스 토큰을 발급한다")
    fun `리프레시 토큰으로 새 액세스 토큰을 발급한다`() {
        // given
        val refreshTokenHeader = "Bearer refresh.token.here"
        val command = RefreshTokenCommand.of(refreshTokenHeader)
        val loginResponse = LoginResponse(
            userId = "1",
            accessToken = "new.access.token",
            refreshToken = "refresh.token.here"
        )

        `when`(refreshTokenUseCase.generateNewAccessToken(command)).thenReturn(loginResponse)

        // when
        val response = controller.refreshToken(refreshTokenHeader)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(loginResponse)
        assertThat(response.data?.accessToken).isEqualTo("new.access.token")
        assertThat(response.data?.refreshToken).isEqualTo("refresh.token.here")
        assertThat(response.data?.userId).isEqualTo("1")
        assertThat(response.message).isEqualTo("새 액세스 토큰이 발급되었습니다.")

        verify(refreshTokenUseCase).generateNewAccessToken(command)
    }
}
