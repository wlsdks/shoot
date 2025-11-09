package com.stark.shoot.domain.social

import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.social.type.FriendRequestStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FriendRequest 도메인 모델 테스트")
class FriendRequestTest {

    private val senderId = UserId.from(1L)
    private val receiverId = UserId.from(2L)

    @Nested
    @DisplayName("accept() 메서드")
    inner class AcceptTest {

        @Test
        @DisplayName("[happy] PENDING 상태의 친구 요청을 수락하면 ACCEPTED 상태로 변경된다")
        fun `PENDING 상태의 친구 요청을 수락하면 ACCEPTED 상태로 변경된다`() {
            // Given: PENDING 상태의 친구 요청
            val friendRequest = FriendRequest.create(senderId, receiverId)
            assertThat(friendRequest.status).isEqualTo(FriendRequestStatus.PENDING)

            // When: 요청 수락
            val result = friendRequest.accept()

            // Then: 상태가 ACCEPTED로 변경됨
            assertThat(friendRequest.status).isEqualTo(FriendRequestStatus.ACCEPTED)
            assertThat(friendRequest.respondedAt).isNotNull
        }

        @Test
        @DisplayName("[happy] 친구 요청 수락 시 FriendshipPair를 반환한다")
        fun `친구 요청 수락 시 FriendshipPair를 반환한다`() {
            // Given
            val friendRequest = FriendRequest.create(senderId, receiverId)

            // When
            val result = friendRequest.accept()

            // Then: FriendshipPair 반환
            assertThat(result).isNotNull
            assertThat(result.friendship1).isNotNull
            assertThat(result.friendship2).isNotNull
            assertThat(result.events).hasSize(2)
        }

        @Test
        @DisplayName("[happy] 친구 요청 수락 시 양방향 Friendship이 생성된다")
        fun `친구 요청 수락 시 양방향 Friendship이 생성된다`() {
            // Given
            val friendRequest = FriendRequest.create(senderId, receiverId)

            // When
            val result = friendRequest.accept()

            // Then: 양방향 Friendship 생성
            val friendships = result.getAllFriendships()
            assertThat(friendships).hasSize(2)

            // receiverId → senderId
            val friendship1 = result.friendship1
            assertThat(friendship1.userId).isEqualTo(receiverId)
            assertThat(friendship1.friendId).isEqualTo(senderId)

            // senderId → receiverId
            val friendship2 = result.friendship2
            assertThat(friendship2.userId).isEqualTo(senderId)
            assertThat(friendship2.friendId).isEqualTo(receiverId)
        }

        @Test
        @DisplayName("[happy] 친구 요청 수락 시 FriendAddedEvent 2개가 생성된다")
        fun `친구 요청 수락 시 FriendAddedEvent 2개가 생성된다`() {
            // Given
            val friendRequest = FriendRequest.create(senderId, receiverId)

            // When
            val result = friendRequest.accept()

            // Then: 2개의 이벤트 생성
            assertThat(result.events).hasSize(2)

            // 각 사용자에 대한 이벤트 확인
            val event1 = result.events[0]
            assertThat(event1.userId).isEqualTo(receiverId)
            assertThat(event1.friendId).isEqualTo(senderId)

            val event2 = result.events[1]
            assertThat(event2.userId).isEqualTo(senderId)
            assertThat(event2.friendId).isEqualTo(receiverId)
        }

        @Test
        @DisplayName("[bad] 이미 ACCEPTED 상태인 요청을 수락하면 예외가 발생한다")
        fun `이미 ACCEPTED 상태인 요청을 수락하면 예외가 발생한다`() {
            // Given: 이미 수락된 요청
            val friendRequest = FriendRequest.create(senderId, receiverId)
            friendRequest.accept()

            // When & Then: 재수락 시 예외 발생
            assertThatThrownBy { friendRequest.accept() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("이미 처리된 친구 요청입니다")
        }

        @Test
        @DisplayName("[bad] 이미 REJECTED 상태인 요청을 수락하면 예외가 발생한다")
        fun `이미 REJECTED 상태인 요청을 수락하면 예외가 발생한다`() {
            // Given: 거절된 요청
            val friendRequest = FriendRequest.create(senderId, receiverId)
            friendRequest.reject()

            // When & Then: 수락 시도 시 예외 발생
            assertThatThrownBy { friendRequest.accept() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("이미 처리된 친구 요청입니다")
        }

        @Test
        @DisplayName("[bad] 이미 CANCELLED 상태인 요청을 수락하면 예외가 발생한다")
        fun `이미 CANCELLED 상태인 요청을 수락하면 예외가 발생한다`() {
            // Given: 취소된 요청
            val friendRequest = FriendRequest.create(senderId, receiverId)
            friendRequest.cancel()

            // When & Then: 수락 시도 시 예외 발생
            assertThatThrownBy { friendRequest.accept() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("이미 처리된 친구 요청입니다")
        }
    }

    @Nested
    @DisplayName("reject() 메서드")
    inner class RejectTest {

        @Test
        @DisplayName("[happy] PENDING 상태의 친구 요청을 거절하면 REJECTED 상태로 변경된다")
        fun `PENDING 상태의 친구 요청을 거절하면 REJECTED 상태로 변경된다`() {
            // Given: PENDING 상태의 친구 요청
            val friendRequest = FriendRequest.create(senderId, receiverId)

            // When: 요청 거절
            friendRequest.reject()

            // Then: 상태가 REJECTED로 변경됨
            assertThat(friendRequest.status).isEqualTo(FriendRequestStatus.REJECTED)
            assertThat(friendRequest.respondedAt).isNotNull
        }

        @Test
        @DisplayName("[bad] 이미 처리된 요청을 거절하면 예외가 발생한다")
        fun `이미 처리된 요청을 거절하면 예외가 발생한다`() {
            // Given: 수락된 요청
            val friendRequest = FriendRequest.create(senderId, receiverId)
            friendRequest.accept()

            // When & Then: 거절 시도 시 예외 발생
            assertThatThrownBy { friendRequest.reject() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("이미 처리된 친구 요청입니다")
        }
    }

    @Nested
    @DisplayName("cancel() 메서드")
    inner class CancelTest {

        @Test
        @DisplayName("[happy] PENDING 상태의 친구 요청을 취소하면 CANCELLED 상태로 변경된다")
        fun `PENDING 상태의 친구 요청을 취소하면 CANCELLED 상태로 변경된다`() {
            // Given: PENDING 상태의 친구 요청
            val friendRequest = FriendRequest.create(senderId, receiverId)

            // When: 요청 취소
            friendRequest.cancel()

            // Then: 상태가 CANCELLED로 변경됨
            assertThat(friendRequest.status).isEqualTo(FriendRequestStatus.CANCELLED)
            assertThat(friendRequest.respondedAt).isNotNull
        }

        @Test
        @DisplayName("[bad] 이미 처리된 요청을 취소하면 예외가 발생한다")
        fun `이미 처리된 요청을 취소하면 예외가 발생한다`() {
            // Given: 수락된 요청
            val friendRequest = FriendRequest.create(senderId, receiverId)
            friendRequest.accept()

            // When & Then: 취소 시도 시 예외 발생
            assertThatThrownBy { friendRequest.cancel() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("이미 처리된 친구 요청입니다")
        }
    }

    @Nested
    @DisplayName("create() 팩토리 메서드")
    inner class CreateTest {

        @Test
        @DisplayName("[happy] 친구 요청을 생성하면 PENDING 상태로 초기화된다")
        fun `친구 요청을 생성하면 PENDING 상태로 초기화된다`() {
            // When
            val friendRequest = FriendRequest.create(senderId, receiverId)

            // Then
            assertThat(friendRequest.senderId).isEqualTo(senderId)
            assertThat(friendRequest.receiverId).isEqualTo(receiverId)
            assertThat(friendRequest.status).isEqualTo(FriendRequestStatus.PENDING)
            assertThat(friendRequest.createdAt).isNotNull
            assertThat(friendRequest.respondedAt).isNull()
        }
    }
}
