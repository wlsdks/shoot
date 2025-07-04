package com.stark.shoot.domain.chat.user

import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FriendRequest 애그리게이트")
class FriendRequestTest {

    @Nested
    @DisplayName("상태 변경")
    inner class StateChange {

        @Test
        @DisplayName("[happy] 친구 요청을 수락하면 ACCEPTED 상태가 된다")
        fun `친구 요청을 수락하면 ACCEPTED 상태가 된다`() {
            val request = FriendRequest(senderId = UserId.from(1L), receiverId = UserId.from(2L))
            val result = request.accept()
            assertThat(result.status).isEqualTo(FriendRequestStatus.ACCEPTED)
            assertThat(result.respondedAt).isNotNull()
        }

        @Test
        @DisplayName("[happy] 친구 요청을 거절하면 REJECTED 상태가 된다")
        fun `친구 요청을 거절하면 REJECTED 상태가 된다`() {
            val request = FriendRequest(senderId = UserId.from(1L), receiverId = UserId.from(2L))
            val result = request.reject()
            assertThat(result.status).isEqualTo(FriendRequestStatus.REJECTED)
            assertThat(result.respondedAt).isNotNull()
        }

        @Test
        @DisplayName("[happy] 친구 요청을 취소하면 CANCELLED 상태가 된다")
        fun `친구 요청을 취소하면 CANCELLED 상태가 된다`() {
            val request = FriendRequest(senderId = UserId.from(1L), receiverId = UserId.from(2L))
            val result = request.cancel()
            assertThat(result.status).isEqualTo(FriendRequestStatus.CANCELLED)
            assertThat(result.respondedAt).isNotNull()
        }
    }
}
