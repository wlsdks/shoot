package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.chat.user.Nickname
import com.stark.shoot.domain.chat.user.UserCode
import com.stark.shoot.domain.chat.user.Username
import com.stark.shoot.domain.event.FriendAddedEvent
import com.stark.shoot.domain.event.FriendRemovedEvent
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.service.FriendDomainService
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
                service.validateFriendRequest(UserId.from(1L), UserId.from(1L), false, false, false)
            }
        }
    }

    @Test
    fun `친구 요청 수락을 처리할 수 있다`() {
        val current = User(id=UserId.from(1L), username=Username.from("a"), nickname=Nickname.from("A"), userCode=UserCode.from("A1"), incomingFriendRequestIds=setOf(UserId.from(2L)))
        val requester = User(id=UserId.from(2L), username=Username.from("b"), nickname=Nickname.from("B"), userCode=UserCode.from("B1"))
        val result = service.processFriendAccept(current, requester, UserId.from(2L))
        assertThat(result.updatedCurrentUser.friendIds).contains(UserId.from(2L))
        assertThat(result.updatedRequester.friendIds).contains(UserId.from(1L))
        assertThat(result.events).containsExactly(
            FriendAddedEvent.create(UserId.from(1L), UserId.from(2L)),
            FriendAddedEvent.create(UserId.from(2L), UserId.from(1L))
        )
    }

    @Test
    fun `친구 관계 삭제를 처리할 수 있다`() {
        val current = User(id=UserId.from(1L), username=Username.from("a"), nickname=Nickname.from("A"), userCode=UserCode.from("A1"), friendIds=setOf(UserId.from(2L)))
        val friend = User(id=UserId.from(2L), username=Username.from("b"), nickname=Nickname.from("B"), userCode=UserCode.from("B1"), friendIds=setOf(UserId.from(1L)))
        val result = service.processFriendRemoval(current, friend, UserId.from(2L))
        assertThat(result.updatedCurrentUser.friendIds).doesNotContain(UserId.from(2L))
        assertThat(result.updatedFriend.friendIds).doesNotContain(UserId.from(1L))
        assertThat(result.events).containsExactly(
            FriendRemovedEvent.create(UserId.from(1L), UserId.from(2L)),
            FriendRemovedEvent.create(UserId.from(2L), UserId.from(1L))
        )
    }
}
