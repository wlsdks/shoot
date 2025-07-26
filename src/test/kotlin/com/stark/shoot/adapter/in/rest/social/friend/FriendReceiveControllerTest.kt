package com.stark.shoot.adapter.`in`.rest.social.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendReceiveUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.AcceptFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.RejectFriendRequestCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@DisplayName("FriendReceiveController 단위 테스트")
class FriendReceiveControllerTest {

    private val friendReceiveUseCase = mock(FriendReceiveUseCase::class.java)
    private val controller = FriendReceiveController(friendReceiveUseCase)

    @Test
    @DisplayName("[happy] 친구 요청을 수락한다")
    fun `친구 요청을 수락한다`() {
        // given
        val userId = 1L
        val requesterId = 2L

        // when
        val response = controller.acceptRequest(userId, requesterId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("친구 요청을 수락했습니다.")

        verify(friendReceiveUseCase).acceptFriendRequest(
            AcceptFriendRequestCommand.of(userId, requesterId)
        )
    }

    @Test
    @DisplayName("[happy] 친구 요청을 거절한다")
    fun `친구 요청을 거절한다`() {
        // given
        val userId = 1L
        val requesterId = 3L

        // when
        val response = controller.rejectRequest(userId, requesterId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("친구 요청을 거절했습니다.")

        verify(friendReceiveUseCase).rejectFriendRequest(
            RejectFriendRequestCommand.of(userId, requesterId)
        )
    }
}
