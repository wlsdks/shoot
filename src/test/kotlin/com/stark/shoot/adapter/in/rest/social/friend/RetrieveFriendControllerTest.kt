package com.stark.shoot.adapter.`in`.rest.social.friend

import com.stark.shoot.adapter.`in`.rest.dto.social.friend.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.GetFriendsCommand
import com.stark.shoot.application.port.`in`.user.friend.command.GetIncomingFriendRequestsCommand
import com.stark.shoot.application.port.`in`.user.friend.command.GetOutgoingFriendRequestsCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

@DisplayName("RetrieveFriendController 단위 테스트")
class RetrieveFriendControllerTest {

    private val findFriendUseCase = mock(FindFriendUseCase::class.java)
    private val controller = RetrieveFriendController(findFriendUseCase)

    @Test
    @DisplayName("[happy] 사용자의 친구 목록을 조회한다")
    fun `사용자의 친구 목록을 조회한다`() {
        // given
        val userId = 1L
        val friends = listOf(
            createFriendResponse(2L, "friend1", "친구1", "http://example.com/profile1.jpg"),
            createFriendResponse(3L, "friend2", "친구2", "http://example.com/profile2.jpg"),
            createFriendResponse(4L, "friend3", "친구3", null)
        )

        val command = GetFriendsCommand.of(userId)
        `when`(findFriendUseCase.getFriends(command)).thenReturn(friends)

        // when
        val response = controller.getMyFriends(userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(3)
        assertThat(response.data).isEqualTo(friends)

        // 첫 번째 친구 검증
        assertThat(response.data?.get(0)?.id).isEqualTo(2L)
        assertThat(response.data?.get(0)?.username).isEqualTo("friend1")
        assertThat(response.data?.get(0)?.nickname).isEqualTo("친구1")
        assertThat(response.data?.get(0)?.profileImageUrl).isEqualTo("http://example.com/profile1.jpg")

        verify(findFriendUseCase).getFriends(command)
    }

    @Test
    @DisplayName("[happy] 사용자가 받은 친구 요청 목록을 조회한다")
    fun `사용자가 받은 친구 요청 목록을 조회한다`() {
        // given
        val userId = 1L
        val incomingRequests = listOf(
            createFriendResponse(5L, "requester1", "요청자1", "http://example.com/profile5.jpg"),
            createFriendResponse(6L, "requester2", "요청자2", null)
        )

        val command = GetIncomingFriendRequestsCommand.of(userId)
        `when`(findFriendUseCase.getIncomingFriendRequests(command)).thenReturn(incomingRequests)

        // when
        val response = controller.getIncomingFriendRequests(userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        assertThat(response.data).isEqualTo(incomingRequests)

        verify(findFriendUseCase).getIncomingFriendRequests(command)
    }

    @Test
    @DisplayName("[happy] 사용자가 보낸 친구 요청 목록을 조회한다")
    fun `사용자가 보낸 친구 요청 목록을 조회한다`() {
        // given
        val userId = 1L
        val outgoingRequests = listOf(
            createFriendResponse(7L, "target1", "대상자1", "http://example.com/profile7.jpg"),
            createFriendResponse(8L, "target2", "대상자2", "http://example.com/profile8.jpg")
        )

        val command = GetOutgoingFriendRequestsCommand.of(userId)
        `when`(findFriendUseCase.getOutgoingFriendRequests(command)).thenReturn(outgoingRequests)

        // when
        val response = controller.getOutgoingFriendRequests(userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        assertThat(response.data).isEqualTo(outgoingRequests)

        verify(findFriendUseCase).getOutgoingFriendRequests(command)
    }

    @Test
    @DisplayName("[happy] 친구가 없는 경우 빈 목록을 반환한다")
    fun `친구가 없는 경우 빈 목록을 반환한다`() {
        // given
        val userId = 1L

        val command = GetFriendsCommand.of(userId)
        `when`(findFriendUseCase.getFriends(command)).thenReturn(emptyList())

        // when
        val response = controller.getMyFriends(userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEmpty()

        verify(findFriendUseCase).getFriends(command)
    }

    // 테스트용 FriendResponse 객체 생성 헬퍼 메서드
    private fun createFriendResponse(
        id: Long,
        username: String,
        nickname: String,
        profileImageUrl: String?
    ): FriendResponse {
        return FriendResponse(
            id = id,
            username = username,
            nickname = nickname,
            profileImageUrl = profileImageUrl
        )
    }
}
