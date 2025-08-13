package com.stark.shoot.domain.chat.user

import com.stark.shoot.domain.user.RefreshToken
import com.stark.shoot.domain.user.vo.RefreshTokenValue
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("리프레시 토큰 테스트")
class RefreshTokenTest {

    @Nested
    @DisplayName("리프레시 토큰 생성 시")
    inner class CreateRefreshToken {

        @Test
        @DisplayName("[happy] 필수 속성으로 리프레시 토큰을 생성할 수 있다")
        fun `필수 속성으로 리프레시 토큰을 생성할 수 있다`() {
            // given
            val userId = UserId.from(1L)
            val token = RefreshTokenValue.from("refresh_token_value")
            val expirationDate = Instant.now().plus(7, ChronoUnit.DAYS)

            // when
            val refreshToken = RefreshToken(
                userId = userId,
                token = token,
                expirationDate = expirationDate
            )

            // then
            assertThat(refreshToken.id).isNull()
            assertThat(refreshToken.userId).isEqualTo(userId)
            assertThat(refreshToken.token).isEqualTo(token)
            assertThat(refreshToken.expirationDate).isEqualTo(expirationDate)
            assertThat(refreshToken.deviceInfo).isNull()
            assertThat(refreshToken.ipAddress).isNull()
            assertThat(refreshToken.createdAt).isNotNull()
            assertThat(refreshToken.lastUsedAt).isNull()
            assertThat(refreshToken.isRevoked).isFalse()
        }

        @Test
        @DisplayName("[happy] 모든 속성으로 리프레시 토큰을 생성할 수 있다")
        fun `모든 속성으로 리프레시 토큰을 생성할 수 있다`() {
            // given
            val id = 1L
            val userId = UserId.from(2L)
            val token = RefreshTokenValue.from("refresh_token_value")
            val expirationDate = Instant.now().plus(7, ChronoUnit.DAYS)
            val deviceInfo = "Android 12, Samsung Galaxy S21"
            val ipAddress = "192.168.1.1"
            val createdAt = Instant.now().minus(1, ChronoUnit.HOURS)
            val lastUsedAt = Instant.now().minus(30, ChronoUnit.MINUTES)
            val isRevoked = false

            // when
            val refreshToken = RefreshToken(
                id = id,
                userId = userId,
                token = token,
                expirationDate = expirationDate,
                deviceInfo = deviceInfo,
                ipAddress = ipAddress,
                createdAt = createdAt,
                lastUsedAt = lastUsedAt,
                isRevoked = isRevoked
            )

            // then
            assertThat(refreshToken.id).isEqualTo(id)
            assertThat(refreshToken.userId).isEqualTo(userId)
            assertThat(refreshToken.token).isEqualTo(token)
            assertThat(refreshToken.expirationDate).isEqualTo(expirationDate)
            assertThat(refreshToken.deviceInfo).isEqualTo(deviceInfo)
            assertThat(refreshToken.ipAddress).isEqualTo(ipAddress)
            assertThat(refreshToken.createdAt).isEqualTo(createdAt)
            assertThat(refreshToken.lastUsedAt).isEqualTo(lastUsedAt)
            assertThat(refreshToken.isRevoked).isEqualTo(isRevoked)
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 유효성 검증 시")
    inner class ValidateRefreshToken {

        @Test
        @DisplayName("[happy] 만료되지 않고 취소되지 않은 토큰은 유효하다")
        fun `만료되지 않고 취소되지 않은 토큰은 유효하다`() {
            // given
            val refreshToken = RefreshToken(
                userId = UserId.from(1L),
                token = RefreshTokenValue.from("valid_token"),
                expirationDate = Instant.now().plus(7, ChronoUnit.DAYS),
                isRevoked = false
            )

            // when
            val isValid = refreshToken.isValid()

            // then
            assertThat(isValid).isTrue()
        }

        @Test
        @DisplayName("[happy] 만료된 토큰은 유효하지 않다")
        fun `만료된 토큰은 유효하지 않다`() {
            // given
            val refreshToken = RefreshToken(
                userId = UserId.from(1L),
                token = RefreshTokenValue.from("expired_token"),
                expirationDate = Instant.now().minus(1, ChronoUnit.DAYS),
                isRevoked = false
            )

            // when
            val isValid = refreshToken.isValid()

            // then
            assertThat(isValid).isFalse()
        }

        @Test
        @DisplayName("[happy] 취소된 토큰은 유효하지 않다")
        fun `취소된 토큰은 유효하지 않다`() {
            // given
            val refreshToken = RefreshToken(
                userId = UserId.from(1L),
                token = RefreshTokenValue.from("revoked_token"),
                expirationDate = Instant.now().plus(7, ChronoUnit.DAYS),
                isRevoked = true
            )

            // when
            val isValid = refreshToken.isValid()

            // then
            assertThat(isValid).isFalse()
        }

        @Test
        @DisplayName("[happy] 만료되고 취소된 토큰은 유효하지 않다")
        fun `만료되고 취소된 토큰은 유효하지 않다`() {
            // given
            val refreshToken = RefreshToken(
                userId = UserId.from(1L),
                token = RefreshTokenValue.from("expired_and_revoked_token"),
                expirationDate = Instant.now().minus(1, ChronoUnit.DAYS),
                isRevoked = true
            )

            // when
            val isValid = refreshToken.isValid()

            // then
            assertThat(isValid).isFalse()
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 사용 시간 업데이트 시")
    inner class UpdateLastUsed {

        @Test
        @DisplayName("[happy] 마지막 사용 시간을 현재 시간으로 업데이트할 수 있다")
        fun `마지막 사용 시간을 현재 시간으로 업데이트할 수 있다`() {
            // given
            val refreshToken = RefreshToken(
                userId = UserId.from(1L),
                token = RefreshTokenValue.from("token"),
                expirationDate = Instant.now().plus(7, ChronoUnit.DAYS)
            )
            val beforeUpdate = Instant.now().minusMillis(100)

            // when
            refreshToken.updateLastUsed()
            val afterUpdate = Instant.now().plusMillis(100)

            // then
            assertThat(refreshToken.lastUsedAt).isNotNull()
            assertThat(refreshToken.lastUsedAt).isAfterOrEqualTo(beforeUpdate)
            assertThat(refreshToken.lastUsedAt).isBeforeOrEqualTo(afterUpdate)
        }

        @Test
        @DisplayName("[happy] 이미 사용 시간이 있는 토큰도 업데이트할 수 있다")
        fun `이미 사용 시간이 있는 토큰도 업데이트할 수 있다`() {
            // given
            val oldLastUsedAt = Instant.now().minus(1, ChronoUnit.HOURS)
            val refreshToken = RefreshToken(
                userId = UserId.from(1L),
                token = RefreshTokenValue.from("token"),
                expirationDate = Instant.now().plus(7, ChronoUnit.DAYS),
                lastUsedAt = oldLastUsedAt
            )
            val beforeUpdate = Instant.now().minusMillis(100)

            // when
            refreshToken.updateLastUsed()
            val afterUpdate = Instant.now().plusMillis(100)

            // then
            assertThat(refreshToken.lastUsedAt).isNotNull()
            assertThat(refreshToken.lastUsedAt).isAfterOrEqualTo(beforeUpdate)
            assertThat(refreshToken.lastUsedAt).isBeforeOrEqualTo(afterUpdate)
            assertThat(refreshToken.lastUsedAt).isAfter(oldLastUsedAt)
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 취소 시")
    inner class RevokeToken {

        @Test
        @DisplayName("[happy] 토큰을 취소할 수 있다")
        fun `토큰을 취소할 수 있다`() {
            // given
            val refreshToken = RefreshToken(
                userId = UserId.from(1L),
                token = RefreshTokenValue.from("token_to_revoke"),
                expirationDate = Instant.now().plus(7, ChronoUnit.DAYS),
                isRevoked = false
            )

            // when
            refreshToken.revoke()

            // then
            assertThat(refreshToken.isRevoked).isTrue()
        }

        @Test
        @DisplayName("[happy] 이미 취소된 토큰도 다시 취소할 수 있다")
        fun `이미 취소된 토큰도 다시 취소할 수 있다`() {
            // given
            val refreshToken = RefreshToken(
                userId = UserId.from(1L),
                token = RefreshTokenValue.from("already_revoked_token"),
                expirationDate = Instant.now().plus(7, ChronoUnit.DAYS),
                isRevoked = true
            )

            // when
            refreshToken.revoke()

            // then
            assertThat(refreshToken.isRevoked).isTrue()
        }
    }
}