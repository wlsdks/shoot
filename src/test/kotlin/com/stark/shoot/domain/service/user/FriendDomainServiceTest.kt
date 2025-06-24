package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.event.FriendAddedEvent
import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.service.FriendDomainService
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("친구 도메인 서비스 테스트")
class FriendDomainServiceTest {
    private val service = FriendDomainService()

    @Nested
    @DisplayName("친구 요청 유효성 검증 시")
    inner class ValidateRequest {
        @Test
        @DisplayName("[bad] 자신에게 요청하면 예외가 발생한다")
        fun `자신에게 요청하면 예외`() {
            assertThrows<IllegalArgumentException> {
                service.validateFriendRequest(UserId.from(1L), UserId.from(1L), false, false, false)
            }
        }

        @Test
        @DisplayName("[bad] 이미 친구인 경우 예외가 발생한다")
        fun `이미 친구인 경우 예외`() {
            assertThrows<IllegalArgumentException> {
                service.validateFriendRequest(UserId.from(1L), UserId.from(2L), true, false, false)
            }
        }

        @Test
        @DisplayName("[bad] 이미 친구 요청을 보낸 경우 예외가 발생한다")
        fun `이미 친구 요청을 보낸 경우 예외`() {
            assertThrows<IllegalArgumentException> {
                service.validateFriendRequest(UserId.from(1L), UserId.from(2L), false, true, false)
            }
        }

        @Test
        @DisplayName("[bad] 상대방이 이미 친구 요청을 보낸 경우 예외가 발생한다")
        fun `상대방이 이미 친구 요청을 보낸 경우 예외`() {
            assertThrows<IllegalArgumentException> {
                service.validateFriendRequest(UserId.from(1L), UserId.from(2L), false, false, true)
            }
        }

        @Test
        @DisplayName("[happy] 유효한 친구 요청은 예외가 발생하지 않는다")
        fun `유효한 친구 요청은 예외가 발생하지 않는다`() {
            // 모든 조건이 false인 경우 (유효한 요청)
            service.validateFriendRequest(UserId.from(1L), UserId.from(2L), false, false, false)
            // 예외가 발생하지 않으면 테스트 통과
        }
    }

    @Test
    @DisplayName("[happy] 친구 요청을 생성할 수 있다")
    fun `친구 요청을 생성할 수 있다`() {
        // given
        val senderId = UserId.from(1L)
        val receiverId = UserId.from(2L)

        // when
        val result = service.createFriendRequest(senderId, receiverId)

        // then
        assertThat(result.senderId).isEqualTo(senderId)
        assertThat(result.receiverId).isEqualTo(receiverId)
        assertThat(result.status).isEqualTo(com.stark.shoot.domain.user.type.FriendRequestStatus.PENDING)
        assertThat(result.respondedAt).isNull()
    }

    @Nested
    @DisplayName("친구 요청 수락 처리 시")
    inner class ProcessFriendAccept {
        @Test
        @DisplayName("[happy] 친구 요청 수락을 처리할 수 있다")
        fun `친구 요청 수락을 처리할 수 있다`() {
            val friendRequest = FriendRequest.create(
                senderId = UserId.from(2L),
                receiverId = UserId.from(1L)
            ).accept() // 친구 요청 수락

            val result = service.processFriendAccept(friendRequest)

            // Check that we have exactly 2 events
            assertThat(result.events).hasSize(2)

            // Check the first event
            assertThat(result.events[0].userId).isEqualTo(UserId.from(1L))
            assertThat(result.events[0].friendId).isEqualTo(UserId.from(2L))

            // Check the second event
            assertThat(result.events[1].userId).isEqualTo(UserId.from(2L))
            assertThat(result.events[1].friendId).isEqualTo(UserId.from(1L))
        }

        @Test
        @DisplayName("[happy] 친구 요청 수락 시 양방향 친구 관계가 생성된다")
        fun `친구 요청 수락 시 양방향 친구 관계가 생성된다`() {
            val friendRequest = FriendRequest.create(
                senderId = UserId.from(2L),
                receiverId = UserId.from(1L)
            ).accept() // 친구 요청 수락

            val result = service.processFriendAccept(friendRequest)

            // Check that we have exactly 2 friendships
            assertThat(result.friendships).hasSize(2)

            // Check the first friendship (receiver -> sender)
            assertThat(result.friendships[0].userId).isEqualTo(UserId.from(1L))
            assertThat(result.friendships[0].friendId).isEqualTo(UserId.from(2L))

            // Check the second friendship (sender -> receiver)
            assertThat(result.friendships[1].userId).isEqualTo(UserId.from(2L))
            assertThat(result.friendships[1].friendId).isEqualTo(UserId.from(1L))
        }

        @Test
        @DisplayName("[happy] 친구 요청 수락 시 요청 상태가 ACCEPTED로 변경된다")
        fun `친구 요청 수락 시 요청 상태가 ACCEPTED로 변경된다`() {
            val friendRequest = FriendRequest.create(
                senderId = UserId.from(2L),
                receiverId = UserId.from(1L)
            ).accept() // 친구 요청 수락

            val result = service.processFriendAccept(friendRequest)

            // Check that the request status is updated
            assertThat(result.updatedRequest.status).isEqualTo(com.stark.shoot.domain.user.type.FriendRequestStatus.ACCEPTED)
            assertThat(result.updatedRequest.respondedAt).isNotNull()
        }
    }

}
