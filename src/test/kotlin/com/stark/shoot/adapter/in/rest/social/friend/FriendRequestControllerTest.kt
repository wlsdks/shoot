package com.stark.shoot.adapter.`in`.rest.social.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.CancelFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.SendFriendRequestCommand
import com.stark.shoot.adapter.`in`.rest.dto.social.friend.SendFriendRequest
import com.stark.shoot.adapter.`in`.rest.dto.social.friend.CancelFriendRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@DisplayName("FriendRequestController 단위 테스트")
class FriendRequestControllerTest {

    private val friendRequestUseCase = mock(FriendRequestUseCase::class.java)
    private val controller = FriendRequestController(friendRequestUseCase)

    @Test
    @DisplayName("[happy] 친구 요청을 보낸다")
    fun `친구 요청을 보낸다`() {
        // given
        val userId = 1L
        val targetUserId = 2L

        val request = SendFriendRequest(userId, targetUserId)

        // when
        val response = controller.sendFriendRequest(request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("친구 요청을 보냈습니다.")

        val command = SendFriendRequestCommand.of(request)
        verify(friendRequestUseCase).sendFriendRequest(command)
    }

    @Test
    @DisplayName("[happy] 보낸 친구 요청을 취소한다")
    fun `보낸 친구 요청을 취소한다`() {
        // given
        val userId = 1L
        val targetUserId = 3L

        val request = CancelFriendRequest(userId, targetUserId)

        // when
        val response = controller.cancelRequest(request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("친구 요청을 취소했습니다.")

        val command = CancelFriendRequestCommand.of(request)
        verify(friendRequestUseCase).cancelFriendRequest(command)
    }
}
