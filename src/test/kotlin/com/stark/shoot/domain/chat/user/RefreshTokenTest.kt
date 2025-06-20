package com.stark.shoot.domain.chat.user

import org.assertj.core.api.Assertions.assertThat
import com.stark.shoot.domain.chat.user.RefreshTokenValue
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
        @DisplayName("필수 속성으로 리프레시 토큰을 생성할 수 있다")
        fun `필수 속성으로 리프레시 토큰을 생성할 수 있다`() {
            // given
            val userId = 1L
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
        @DisplayName("모든 속성으로 리프레시 토큰을 생성할 수 있다")
        fun `모든 속성으로 리프레시 토큰을 생성할 수 있다`() {
            // given
            val id = 1L
            val userId = 2L
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
        @DisplayName("만료되지 않고 취소되지 않은 토큰은 유효하다")
        fun `만료되지 않고 취소되지 않은 토큰은 유효하다`() {
            // given
            val refreshToken = RefreshToken(
                userId = 1L,
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
        @DisplayName("만료된 토큰은 유효하지 않다")
        fun `만료된 토큰은 유효하지 않다`() {
            // given
            val refreshToken = RefreshToken(
                userId = 1L,
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
        @DisplayName("취소된 토큰은 유효하지 않다")
        fun `취소된 토큰은 유효하지 않다`() {
            // given
            val refreshToken = RefreshToken(
                userId = 1L,
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
        @DisplayName("만료되고 취소된 토큰은 유효하지 않다")
        fun `만료되고 취소된 토큰은 유효하지 않다`() {
            // given
            val refreshToken = RefreshToken(
                userId = 1L,
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
        @DisplayName("마지막 사용 시간을 현재 시간으로 업데이트할 수 있다")
        fun `마지막 사용 시간을 현재 시간으로 업데이트할 수 있다`() {
            // given
            val refreshToken = RefreshToken(
                userId = 1L,
                token = RefreshTokenValue.from("token"),
                expirationDate = Instant.now().plus(7, ChronoUnit.DAYS)
            )
            val beforeUpdate = Instant.now().minusMillis(100)
            
            // when
            val updatedToken = refreshToken.updateLastUsed()
            val afterUpdate = Instant.now().plusMillis(100)
            
            // then
            assertThat(updatedToken.lastUsedAt).isNotNull()
            assertThat(updatedToken.lastUsedAt).isAfterOrEqualTo(beforeUpdate)
            assertThat(updatedToken.lastUsedAt).isBeforeOrEqualTo(afterUpdate)
            
            // 다른 속성은 변경되지 않아야 함
            assertThat(updatedToken.id).isEqualTo(refreshToken.id)
            assertThat(updatedToken.userId).isEqualTo(refreshToken.userId)
            assertThat(updatedToken.token).isEqualTo(refreshToken.token)
            assertThat(updatedToken.expirationDate).isEqualTo(refreshToken.expirationDate)
            assertThat(updatedToken.deviceInfo).isEqualTo(refreshToken.deviceInfo)
            assertThat(updatedToken.ipAddress).isEqualTo(refreshToken.ipAddress)
            assertThat(updatedToken.createdAt).isEqualTo(refreshToken.createdAt)
            assertThat(updatedToken.isRevoked).isEqualTo(refreshToken.isRevoked)
        }
        
        @Test
        @DisplayName("이미 사용 시간이 있는 토큰도 업데이트할 수 있다")
        fun `이미 사용 시간이 있는 토큰도 업데이트할 수 있다`() {
            // given
            val oldLastUsedAt = Instant.now().minus(1, ChronoUnit.HOURS)
            val refreshToken = RefreshToken(
                userId = 1L,
                token = RefreshTokenValue.from("token"),
                expirationDate = Instant.now().plus(7, ChronoUnit.DAYS),
                lastUsedAt = oldLastUsedAt
            )
            val beforeUpdate = Instant.now().minusMillis(100)
            
            // when
            val updatedToken = refreshToken.updateLastUsed()
            val afterUpdate = Instant.now().plusMillis(100)
            
            // then
            assertThat(updatedToken.lastUsedAt).isNotNull()
            assertThat(updatedToken.lastUsedAt).isAfterOrEqualTo(beforeUpdate)
            assertThat(updatedToken.lastUsedAt).isBeforeOrEqualTo(afterUpdate)
            assertThat(updatedToken.lastUsedAt).isAfter(oldLastUsedAt)
        }
    }
    
    @Nested
    @DisplayName("리프레시 토큰 취소 시")
    inner class RevokeToken {
    
        @Test
        @DisplayName("토큰을 취소할 수 있다")
        fun `토큰을 취소할 수 있다`() {
            // given
            val refreshToken = RefreshToken(
                userId = 1L,
                token = RefreshTokenValue.from("token_to_revoke"),
                expirationDate = Instant.now().plus(7, ChronoUnit.DAYS),
                isRevoked = false
            )
            
            // when
            val revokedToken = refreshToken.revoke()
            
            // then
            assertThat(revokedToken.isRevoked).isTrue()
            
            // 다른 속성은 변경되지 않아야 함
            assertThat(revokedToken.id).isEqualTo(refreshToken.id)
            assertThat(revokedToken.userId).isEqualTo(refreshToken.userId)
            assertThat(revokedToken.token).isEqualTo(refreshToken.token)
            assertThat(revokedToken.expirationDate).isEqualTo(refreshToken.expirationDate)
            assertThat(revokedToken.deviceInfo).isEqualTo(refreshToken.deviceInfo)
            assertThat(revokedToken.ipAddress).isEqualTo(refreshToken.ipAddress)
            assertThat(revokedToken.createdAt).isEqualTo(refreshToken.createdAt)
            assertThat(revokedToken.lastUsedAt).isEqualTo(refreshToken.lastUsedAt)
        }
        
        @Test
        @DisplayName("이미 취소된 토큰도 다시 취소할 수 있다")
        fun `이미 취소된 토큰도 다시 취소할 수 있다`() {
            // given
            val refreshToken = RefreshToken(
                userId = 1L,
                token = RefreshTokenValue.from("already_revoked_token"),
                expirationDate = Instant.now().plus(7, ChronoUnit.DAYS),
                isRevoked = true
            )
            
            // when
            val revokedToken = refreshToken.revoke()
            
            // then
            assertThat(revokedToken.isRevoked).isTrue()
            assertThat(revokedToken).isEqualTo(refreshToken)
        }
    }
}