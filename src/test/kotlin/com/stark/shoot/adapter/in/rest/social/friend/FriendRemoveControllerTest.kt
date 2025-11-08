package com.stark.shoot.adapter.`in`.rest.social.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRemoveUseCase
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.application.port.`in`.user.friend.command.RemoveFriendCommand
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.user.vo.*
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import com.stark.shoot.domain.shared.UserId
import org.junit.jupiter.api.DisplayName
import com.stark.shoot.domain.shared.UserId
import org.junit.jupiter.api.Test
import com.stark.shoot.domain.shared.UserId
import org.mockito.Mockito.*
import com.stark.shoot.domain.shared.UserId
import org.springframework.security.core.Authentication
import com.stark.shoot.domain.shared.UserId
import java.time.Instant
import com.stark.shoot.domain.shared.UserId

@DisplayName("FriendRemoveController 단위 테스트")
class FriendRemoveControllerTest {

    private val friendRemoveUseCase = mock(FriendRemoveUseCase::class.java)
    private val authentication = mock(Authentication::class.java)
    private val controller = FriendRemoveController(friendRemoveUseCase)

    @Test
    @DisplayName("[happy] 친구를 삭제한다")
    fun `친구를 삭제한다`() {
        // given
        val userId = 1L
        val friendId = 2L

        val user = createUser(userId, "testuser", "테스트 유저", "TEST123")

        `when`(authentication.name).thenReturn(userId.toString())
        val command = RemoveFriendCommand.of(userId, friendId)
        `when`(friendRemoveUseCase.removeFriend(command))
            .thenReturn(user)

        // when
        val response = controller.removeFriend(authentication, friendId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(userId.toString())
        assertThat(response.data?.username).isEqualTo("testuser")
        assertThat(response.data?.nickname).isEqualTo("테스트 유저")
        assertThat(response.message).isEqualTo("친구가 삭제되었습니다.")

        verify(authentication).name
        verify(friendRemoveUseCase).removeFriend(command)
    }

    // 테스트용 User 객체 생성 헬퍼 메서드
    private fun createUser(
        id: Long,
        username: String,
        nickname: String,
        userCode: String,
        status: UserStatus = UserStatus.ONLINE,
        bio: String? = null,
        profileImageUrl: String? = null,
        backgroundImageUrl: String? = null
    ): User {
        return User(
            id = UserId.from(id),
            username = Username.from(username),
            nickname = Nickname.from(nickname),
            status = status,
            userCode = UserCode.from(userCode),
            bio = bio?.let { UserBio.from(it) },
            profileImageUrl = profileImageUrl?.let { ProfileImageUrl.from(it) },
            backgroundImageUrl = backgroundImageUrl?.let { BackgroundImageUrl.from(it) },
            lastSeenAt = Instant.now()
        )
    }
}
