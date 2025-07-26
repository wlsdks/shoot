package com.stark.shoot.adapter.`in`.rest.social.recommand

import com.stark.shoot.adapter.`in`.rest.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.RecommendFriendsUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.GetRecommendedFriendsCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

@DisplayName("UserRecommendController 단위 테스트")
class UserRecommendControllerTest {

    private val recommendFriendsUseCase = mock(RecommendFriendsUseCase::class.java)
    private val controller = UserRecommendController(recommendFriendsUseCase)

    @Test
    @DisplayName("[happy] BFS를 통해 친구를 추천한다")
    fun `BFS를 통해 친구를 추천한다`() {
        // given
        val userId = 1L
        val limit = 10
        val maxDepth = 2
        val skip = 0

        val recommendations = listOf(
            createFriendResponse(2L, "friend1", "친구1", "http://example.com/profile1.jpg"),
            createFriendResponse(3L, "friend2", "친구2", "http://example.com/profile2.jpg"),
            createFriendResponse(4L, "friend3", "친구3", null)
        )

        val command = GetRecommendedFriendsCommand.of(userId, skip, limit)
        `when`(recommendFriendsUseCase.getRecommendedFriends(command)).thenReturn(recommendations)

        // when
        val response = controller.recommendFriendsBFS(userId, limit, maxDepth, skip)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(3)
        assertThat(response.data).isEqualTo(recommendations)

        // 첫 번째 추천 친구 검증
        assertThat(response.data?.get(0)?.id).isEqualTo(2L)
        assertThat(response.data?.get(0)?.username).isEqualTo("friend1")
        assertThat(response.data?.get(0)?.nickname).isEqualTo("친구1")
        assertThat(response.data?.get(0)?.profileImageUrl).isEqualTo("http://example.com/profile1.jpg")

        verify(recommendFriendsUseCase).getRecommendedFriends(command)
    }

    @Test
    @DisplayName("[happy] 추천할 친구가 없는 경우 빈 목록을 반환한다")
    fun `추천할 친구가 없는 경우 빈 목록을 반환한다`() {
        // given
        val userId = 1L
        val limit = 10
        val maxDepth = 2
        val skip = 0

        val command = GetRecommendedFriendsCommand.of(userId, skip, limit)
        `when`(recommendFriendsUseCase.getRecommendedFriends(command)).thenReturn(emptyList())

        // when
        val response = controller.recommendFriendsBFS(userId, limit, maxDepth, skip)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEmpty()

        verify(recommendFriendsUseCase).getRecommendedFriends(command)
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
