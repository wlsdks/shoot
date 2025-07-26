package com.stark.shoot.adapter.`in`.rest.social.search

import com.stark.shoot.adapter.`in`.rest.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FriendSearchUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.SearchFriendsCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

@DisplayName("FriendSearchController 단위 테스트")
class FriendSearchControllerTest {

    private val friendSearchUseCase = mock(FriendSearchUseCase::class.java)
    private val controller = FriendSearchController(friendSearchUseCase)

    @Test
    @DisplayName("[happy] 친구 목록에서 검색어와 일치하는 친구들을 찾는다")
    fun `친구 목록에서 검색어와 일치하는 친구들을 찾는다`() {
        // given
        val userId = 1L
        val query = "친구"

        val searchResults = listOf(
            createFriendResponse(2L, "friend1", "친구1", "http://example.com/profile1.jpg"),
            createFriendResponse(3L, "friend2", "친구2", "http://example.com/profile2.jpg")
        )

        val command = SearchFriendsCommand.of(userId, query)
        `when`(friendSearchUseCase.searchPotentialFriends(command))
            .thenReturn(searchResults)

        // when
        val response = controller.searchFriends(userId, query)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        assertThat(response.data).isEqualTo(searchResults)

        // 첫 번째 검색 결과 검증
        assertThat(response.data?.get(0)?.id).isEqualTo(2L)
        assertThat(response.data?.get(0)?.username).isEqualTo("friend1")
        assertThat(response.data?.get(0)?.nickname).isEqualTo("친구1")
        assertThat(response.data?.get(0)?.profileImageUrl).isEqualTo("http://example.com/profile1.jpg")

        verify(friendSearchUseCase).searchPotentialFriends(command)
    }

    @Test
    @DisplayName("[happy] 검색 결과가 없는 경우 빈 목록을 반환한다")
    fun `검색 결과가 없는 경우 빈 목록을 반환한다`() {
        // given
        val userId = 1L
        val query = "존재하지 않는 친구"

        val command = SearchFriendsCommand.of(userId, query)
        `when`(friendSearchUseCase.searchPotentialFriends(command))
            .thenReturn(emptyList())

        // when
        val response = controller.searchFriends(userId, query)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEmpty()

        verify(friendSearchUseCase).searchPotentialFriends(command)
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
