package com.stark.shoot.domain.chat.user

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.domain.user.vo.Nickname
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.user.vo.Username
import com.stark.shoot.infrastructure.exception.InvalidUserDataException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("사용자 테스트")
class UserTest {

    @Nested
    @DisplayName("사용자 생성 시")
    inner class CreateUser {

        @Test
        @DisplayName("[happy] 유효한 정보로 사용자를 생성할 수 있다")
        fun `유효한 정보로 사용자를 생성할 수 있다`() {
            // given
            val username = "testuser"
            val nickname = "테스트유저"
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }

            // when
            val user = User.create(
                username = username,
                nickname = nickname,
                rawPassword = rawPassword,
                passwordEncoder = passwordEncoder
            )

            // then
            assertThat(user.username.value).isEqualTo(username)
            assertThat(user.nickname.value).isEqualTo(nickname)
            assertThat(user.passwordHash).isEqualTo("encoded_$rawPassword")
            assertThat(user.status).isEqualTo(UserStatus.OFFLINE)
            assertThat(user.userCode.value).isNotEmpty()
            assertThat(user.userCode.value.length).isEqualTo(8)
            assertThat(user.createdAt).isNotNull()
            assertThat(user.isDeleted).isFalse()
        }

        @Test
        @DisplayName("[happy] 선택적 정보를 포함하여 사용자를 생성할 수 있다")
        fun `선택적 정보를 포함하여 사용자를 생성할 수 있다`() {
            // given
            val username = "testuser"
            val nickname = "테스트유저"
            val rawPassword = "password123"
            val bio = "안녕하세요"
            val profileImageUrl = "https://example.com/profile.jpg"
            val passwordEncoder: (String) -> String = { "encoded_$it" }

            // when
            val user = User.create(
                username = username,
                nickname = nickname,
                rawPassword = rawPassword,
                passwordEncoder = passwordEncoder,
                bio = bio,
                profileImageUrl = profileImageUrl
            )

            // then
            assertThat(user.username.value).isEqualTo(username)
            assertThat(user.nickname.value).isEqualTo(nickname)
            assertThat(user.passwordHash).isEqualTo("encoded_$rawPassword")
            assertThat(user.bio?.value).isEqualTo(bio)
            assertThat(user.profileImageUrl?.value).isEqualTo(profileImageUrl)
        }

        @Test
        @DisplayName("[happy] 사용자명이 너무 짧으면 예외가 발생한다")
        fun `사용자명이 너무 짧으면 예외가 발생한다`() {
            // given
            val username = "ab" // 3자 미만
            val nickname = "테스트유저"
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }

            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }

            assertThat(exception.message).contains("사용자명은 3-20자 사이여야 합니다")
        }

        @Test
        @DisplayName("[happy] 사용자명이 너무 길면 예외가 발생한다")
        fun `사용자명이 너무 길면 예외가 발생한다`() {
            // given
            val username = "a".repeat(21) // 20자 초과
            val nickname = "테스트유저"
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }

            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }

            assertThat(exception.message).contains("사용자명은 3-20자 사이여야 합니다")
        }

        @Test
        @DisplayName("[happy] 사용자명이 비어있으면 예외가 발생한다")
        fun `사용자명이 비어있으면 예외가 발생한다`() {
            // given
            val username = ""
            val nickname = "테스트유저"
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }

            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }

            assertThat(exception.message).contains("사용자명은 비어있을 수 없습니다")
        }

        @Test
        @DisplayName("[happy] 닉네임이 너무 짧으면 예외가 발생한다")
        fun `닉네임이 너무 짧으면 예외가 발생한다`() {
            // given
            val username = "testuser"
            val nickname = "a" // 2자 미만
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }

            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }

            assertThat(exception.message).contains("닉네임은 2-30자 사이여야 합니다")
        }

        @Test
        @DisplayName("[happy] 닉네임이 너무 길면 예외가 발생한다")
        fun `닉네임이 너무 길면 예외가 발생한다`() {
            // given
            val username = "testuser"
            val nickname = "a".repeat(31) // 30자 초과
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }

            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }

            assertThat(exception.message).contains("닉네임은 2-30자 사이여야 합니다")
        }

        @Test
        @DisplayName("[happy] 닉네임이 비어있으면 예외가 발생한다")
        fun `닉네임이 비어있으면 예외가 발생한다`() {
            // given
            val username = "testuser"
            val nickname = ""
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }

            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }

            assertThat(exception.message).contains("닉네임은 비어있을 수 없습니다")
        }

        @Test
        @DisplayName("[happy] 비밀번호가 너무 짧으면 예외가 발생한다")
        fun `비밀번호가 너무 짧으면 예외가 발생한다`() {
            // given
            val username = "testuser"
            val nickname = "테스트유저"
            val rawPassword = "pass" // 8자 미만
            val passwordEncoder: (String) -> String = { "encoded_$it" }

            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }

            assertThat(exception.message).contains("비밀번호는 최소 8자 이상이어야 합니다")
        }

        @Test
        @DisplayName("[happy] 비밀번호가 비어있으면 예외가 발생한다")
        fun `비밀번호가 비어있으면 예외가 발생한다`() {
            // given
            val username = "testuser"
            val nickname = "테스트유저"
            val rawPassword = ""
            val passwordEncoder: (String) -> String = { "encoded_$it" }

            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }

            assertThat(exception.message).contains("비밀번호는 비어있을 수 없습니다")
        }
    }

    @Nested
    @DisplayName("계정 관리 시")
    inner class ManageAccount {

        @Test
        @DisplayName("[happy] 계정을 삭제할 수 있다")
        fun `계정을 삭제할 수 있다`() {
            // given
            val user = User(
                id = UserId.from(1L),
                username = Username.from("testuser"),
                nickname = Nickname.from("테스트유저"),
                userCode = UserCode.from("ABCD1234"),
                status = UserStatus.ONLINE
            )

            // when
            user.markAsDeleted()

            // then
            assertThat(user.isDeleted).isTrue()
            assertThat(user.status).isEqualTo(UserStatus.OFFLINE)
            assertThat(user.updatedAt).isNotNull()
        }
    }
}