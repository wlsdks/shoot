package com.stark.shoot.domain.user.vo

import com.stark.shoot.infrastructure.exception.InvalidUserDataException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.hamcrest.Matchers.hasSize

@DisplayName("사용자 VO 테스트")
class UserValueObjectsTest {

    @Nested
    inner class UserIdTest {
        @Test
        @DisplayName("[happy] 양수로 생성")
        fun `양수로 생성`() {
            val id = UserId.from(1L)
            assertThat(id.value).isEqualTo(1L)
        }

        @Test
        @DisplayName("[bad] 0 이하는 예외")
        fun `0 이하는 예외`() {
            assertThatThrownBy { UserId.from(0) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class UsernameTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val u = Username.from("user")
            assertThat(u.value).isEqualTo("user")
        }

        @Test
        @DisplayName("[bad] 빈값 예외")
        fun `빈값 예외`() {
            assertThatThrownBy { Username.from(" ") }
                .isInstanceOf(InvalidUserDataException::class.java)
        }

        @Test
        @DisplayName("[bad] 길이 제한 예외")
        fun `길이 제한 예외`() {
            assertThatThrownBy { Username.from("ab") }
                .isInstanceOf(InvalidUserDataException::class.java)
        }
    }

    @Nested
    inner class NicknameTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val n = Nickname.from("닉네임")
            assertThat(n.value).isEqualTo("닉네임")
        }

        @Test
        @DisplayName("[bad] 빈값 예외")
        fun `빈값 예외`() {
            assertThatThrownBy { Nickname.from("") }
                .isInstanceOf(InvalidUserDataException::class.java)
        }

        @Test
        @DisplayName("[bad] 길이 제한 예외")
        fun `길이 제한 예외`() {
            assertThatThrownBy { Nickname.from("a") }
                .isInstanceOf(InvalidUserDataException::class.java)
        }
    }

    @Nested
    inner class UserCodeTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val code = UserCode.from("ABCD")
            assertThat(code.value).isEqualTo("ABCD")
        }

        @Test
        @DisplayName("[bad] 패턴 불일치 예외")
        fun `패턴 불일치 예외`() {
            assertThatThrownBy { UserCode.from("ab") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class ProfileImageUrlTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val url = ProfileImageUrl.from("https://a.com/img.png")
            assertThat(url.value).isEqualTo("https://a.com/img.png")
        }

        @Test
        @DisplayName("[happy] URL 프로토콜 없이 생성")
        fun `URL 프로토콜 없이 생성`() {
            val url = ProfileImageUrl.from("example.com/img.png")
            assertThat(url.value).isEqualTo("example.com/img.png")
        }
    }

    @Nested
    inner class BackgroundImageUrlTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val url = BackgroundImageUrl.from("https://a.com/bg.png")
            assertThat(url.value).isEqualTo("https://a.com/bg.png")
        }

        @Test
        @DisplayName("[happy] URL 프로토콜 없이 생성")
        fun `URL 프로토콜 없이 생성`() {
            val url = BackgroundImageUrl.from("example.com/bg.png")
            assertThat(url.value).isEqualTo("example.com/bg.png")
        }
    }

    @Nested
    inner class UserBioTest {
        @Test
        @DisplayName("[happy] 200자 이내 생성")
        fun `200자 이내 생성`() {
            val bio = UserBio.from("a".repeat(200))
            assertThat(bio.value).hasSize(200)
        }

        @Test
        @DisplayName("[bad] 200자 초과 예외")
        fun `200자 초과 예외`() {
            val long = "a".repeat(201)
            assertThatThrownBy { UserBio.from(long) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class FriendGroupNameTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val name = FriendGroupName.from("친구")
            assertThat(name.value).isEqualTo("친구")
        }

        @Test
        @DisplayName("[bad] 빈값 예외")
        fun `빈값 예외`() {
            assertThatThrownBy { FriendGroupName.from(" ") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("[bad] 길이초과 예외")
        fun `길이초과 예외`() {
            val long = "a".repeat(51)
            assertThatThrownBy { FriendGroupName.from(long) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class RefreshTokenValueTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val token = RefreshTokenValue.from("token")
            assertThat(token.value).isEqualTo("token")
        }

        @Test
        @DisplayName("[bad] blank 예외")
        fun `blank 예외`() {
            assertThatThrownBy { RefreshTokenValue.from("") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
