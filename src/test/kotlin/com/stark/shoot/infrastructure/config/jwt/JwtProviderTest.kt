package com.stark.shoot.infrastructure.config.jwt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("JwtProvider 테스트")
class JwtProviderTest {

    private val jwtProvider = JwtProvider(
        secret = "mysecretkeymysecretkeymysecretkey12",
        expiration = 60,
        refreshExpiration = 120,
        issuer = "shoot-app",
        audience = "shoot-clients"
    )

    @Test
    fun `토큰을 생성하고 값을 추출할 수 있다`() {
        val token = jwtProvider.generateToken("1", "user")

        assertThat(jwtProvider.isTokenValid(token)).isTrue()
        assertThat(jwtProvider.extractId(token)).isEqualTo("1")
        assertThat(jwtProvider.extractUsername(token)).isEqualTo("user")
    }

    @Test
    fun `리프레시 토큰을 생성하고 검증할 수 있다`() {
        val refresh = jwtProvider.generateRefreshToken("1", "user")

        assertThat(jwtProvider.isRefreshTokenValid(refresh)).isTrue()
        assertThat(jwtProvider.isTokenValid(refresh.value)).isFalse()
    }

    @Test
    fun `잘못된 토큰은 유효하지 않다`() {
        assertThat(jwtProvider.isTokenValid("wrong.token.value")).isFalse()
    }
}
