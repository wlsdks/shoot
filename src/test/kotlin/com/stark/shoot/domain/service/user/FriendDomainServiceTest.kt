package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.chat.event.FriendAddedEvent
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.domain.chat.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("친구 도메인 서비스 테스트")
class FriendDomainServiceTest {
    private val service = FriendDomainService()

    @Nested
    inner class ValidateRequest {
        @Test
        fun `자신에게 요청하면 예외`() {
            assertThrows<IllegalArgumentException> {
                service.validateFriendRequest(1L,1L,false,false,false)
            }
        }
    }

    @Test
    fun `친구 요청 수락을 처리할 수 있다`() {
        val current = User(id=1L, username="a", nickname="A", userCode="A1", incomingFriendRequestIds=setOf(2L))
        val requester = User(id=2L, username="b", nickname="B", userCode="B1")
        val result = service.processFriendAccept(current, requester, 2L)
        assertThat(result.updatedCurrentUser.friendIds).contains(2L)
        assertThat(result.updatedRequester.friendIds).contains(1L)
        assertThat(result.events).containsExactly(
            FriendAddedEvent.create(1L,2L), FriendAddedEvent.create(2L,1L)
        )
    }
}
