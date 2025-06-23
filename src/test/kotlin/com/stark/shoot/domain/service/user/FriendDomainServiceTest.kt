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
        val friendRequest = FriendRequest.create(
            senderId = UserId.from(2L),
            receiverId = UserId.from(1L)
        ).accept() // 친구 요청 수락

        val result = service.processFriendAccept(friendRequest)

        assertThat(result.events).containsExactly(
            FriendAddedEvent.create(UserId.from(1L), UserId.from(2L)),
            FriendAddedEvent.create(UserId.from(2L), UserId.from(1L))
        )
    }

}
