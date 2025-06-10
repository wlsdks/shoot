package com.stark.shoot.domain.chat.user

import com.stark.shoot.domain.chat.user.UserStatus
import com.stark.shoot.domain.exception.InvalidUserDataException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("사용자 테스트")
class UserTest {

    @Nested
    @DisplayName("사용자 생성 시")
    inner class CreateUser {
    
        @Test
        @DisplayName("유효한 정보로 사용자를 생성할 수 있다")
        fun `유효한 정보로 사용자를 생성할 수 있다`() {
            // given
            val username = "testuser"
            val nickname = "테스트유저"
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }
            
            // when
            val user = User.create(
                username = username,
                nickname = nickname,
                rawPassword = rawPassword,
                passwordEncoder = passwordEncoder
            )
            
            // then
            assertThat(user.username).isEqualTo(username)
            assertThat(user.nickname).isEqualTo(nickname)
            assertThat(user.passwordHash).isEqualTo("encoded_$rawPassword")
            assertThat(user.status).isEqualTo(UserStatus.OFFLINE)
            assertThat(user.userCode).isNotEmpty()
            assertThat(user.userCode.length).isEqualTo(8)
            assertThat(user.createdAt).isNotNull()
            assertThat(user.isDeleted).isFalse()
            assertThat(user.friendIds).isEmpty()
            assertThat(user.incomingFriendRequestIds).isEmpty()
            assertThat(user.outgoingFriendRequestIds).isEmpty()
        }
        
        @Test
        @DisplayName("선택적 정보를 포함하여 사용자를 생성할 수 있다")
        fun `선택적 정보를 포함하여 사용자를 생성할 수 있다`() {
            // given
            val username = "testuser"
            val nickname = "테스트유저"
            val rawPassword = "password123"
            val bio = "안녕하세요"
            val profileImageUrl = "https://example.com/profile.jpg"
            val passwordEncoder: (String) -> String = { "encoded_$it" }
            
            // when
            val user = User.create(
                username = username,
                nickname = nickname,
                rawPassword = rawPassword,
                passwordEncoder = passwordEncoder,
                bio = bio,
                profileImageUrl = profileImageUrl
            )
            
            // then
            assertThat(user.username).isEqualTo(username)
            assertThat(user.nickname).isEqualTo(nickname)
            assertThat(user.passwordHash).isEqualTo("encoded_$rawPassword")
            assertThat(user.bio).isEqualTo(bio)
            assertThat(user.profileImageUrl).isEqualTo(profileImageUrl)
        }
        
        @Test
        @DisplayName("사용자명이 너무 짧으면 예외가 발생한다")
        fun `사용자명이 너무 짧으면 예외가 발생한다`() {
            // given
            val username = "ab" // 3자 미만
            val nickname = "테스트유저"
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }
            
            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }
            
            assertThat(exception.message).contains("사용자명은 3-20자 사이여야 합니다")
        }
        
        @Test
        @DisplayName("사용자명이 너무 길면 예외가 발생한다")
        fun `사용자명이 너무 길면 예외가 발생한다`() {
            // given
            val username = "a".repeat(21) // 20자 초과
            val nickname = "테스트유저"
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }
            
            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }
            
            assertThat(exception.message).contains("사용자명은 3-20자 사이여야 합니다")
        }
        
        @Test
        @DisplayName("사용자명이 비어있으면 예외가 발생한다")
        fun `사용자명이 비어있으면 예외가 발생한다`() {
            // given
            val username = ""
            val nickname = "테스트유저"
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }
            
            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }
            
            assertThat(exception.message).contains("사용자명은 비어있을 수 없습니다")
        }
        
        @Test
        @DisplayName("닉네임이 너무 짧으면 예외가 발생한다")
        fun `닉네임이 너무 짧으면 예외가 발생한다`() {
            // given
            val username = "testuser"
            val nickname = "a" // 2자 미만
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }
            
            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }
            
            assertThat(exception.message).contains("닉네임은 2-30자 사이여야 합니다")
        }
        
        @Test
        @DisplayName("닉네임이 너무 길면 예외가 발생한다")
        fun `닉네임이 너무 길면 예외가 발생한다`() {
            // given
            val username = "testuser"
            val nickname = "a".repeat(31) // 30자 초과
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }
            
            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }
            
            assertThat(exception.message).contains("닉네임은 2-30자 사이여야 합니다")
        }
        
        @Test
        @DisplayName("닉네임이 비어있으면 예외가 발생한다")
        fun `닉네임이 비어있으면 예외가 발생한다`() {
            // given
            val username = "testuser"
            val nickname = ""
            val rawPassword = "password123"
            val passwordEncoder: (String) -> String = { "encoded_$it" }
            
            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }
            
            assertThat(exception.message).contains("닉네임은 비어있을 수 없습니다")
        }
        
        @Test
        @DisplayName("비밀번호가 너무 짧으면 예외가 발생한다")
        fun `비밀번호가 너무 짧으면 예외가 발생한다`() {
            // given
            val username = "testuser"
            val nickname = "테스트유저"
            val rawPassword = "pass" // 8자 미만
            val passwordEncoder: (String) -> String = { "encoded_$it" }
            
            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }
            
            assertThat(exception.message).contains("비밀번호는 최소 8자 이상이어야 합니다")
        }
        
        @Test
        @DisplayName("비밀번호가 비어있으면 예외가 발생한다")
        fun `비밀번호가 비어있으면 예외가 발생한다`() {
            // given
            val username = "testuser"
            val nickname = "테스트유저"
            val rawPassword = ""
            val passwordEncoder: (String) -> String = { "encoded_$it" }
            
            // when & then
            val exception = assertThrows<InvalidUserDataException> {
                User.create(
                    username = username,
                    nickname = nickname,
                    rawPassword = rawPassword,
                    passwordEncoder = passwordEncoder
                )
            }
            
            assertThat(exception.message).contains("비밀번호는 비어있을 수 없습니다")
        }
    }
    
    @Nested
    @DisplayName("친구 관리 시")
    inner class ManageFriends {
    
        @Test
        @DisplayName("친구를 추가할 수 있다")
        fun `친구를 추가할 수 있다`() {
            // given
            val user = User(
                id = 1L,
                username = "testuser",
                nickname = "테스트유저",
                userCode = "ABCD1234"
            )
            val friendId = 2L
            
            // when
            val updatedUser = user.addFriend(friendId)
            
            // then
            assertThat(updatedUser.friendIds).contains(friendId)
            assertThat(updatedUser.updatedAt).isNotNull()
        }
        
        @Test
        @DisplayName("친구 추가 시 보낸 요청과 받은 요청에서 제거된다")
        fun `친구 추가 시 보낸 요청과 받은 요청에서 제거된다`() {
            // given
            val user = User(
                id = 1L,
                username = "testuser",
                nickname = "테스트유저",
                userCode = "ABCD1234",
                outgoingFriendRequestIds = setOf(2L, 3L),
                incomingFriendRequestIds = setOf(2L, 4L)
            )
            val friendId = 2L
            
            // when
            val updatedUser = user.addFriend(friendId)
            
            // then
            assertThat(updatedUser.friendIds).contains(friendId)
            assertThat(updatedUser.outgoingFriendRequestIds).doesNotContain(friendId)
            assertThat(updatedUser.incomingFriendRequestIds).doesNotContain(friendId)
            assertThat(updatedUser.outgoingFriendRequestIds).contains(3L)
            assertThat(updatedUser.incomingFriendRequestIds).contains(4L)
        }
        
        @Test
        @DisplayName("받은 친구 요청을 수락할 수 있다")
        fun `받은 친구 요청을 수락할 수 있다`() {
            // given
            val user = User(
                id = 1L,
                username = "testuser",
                nickname = "테스트유저",
                userCode = "ABCD1234",
                incomingFriendRequestIds = setOf(2L, 3L)
            )
            val requesterId = 2L
            
            // when
            val updatedUser = user.acceptFriendRequest(requesterId)
            
            // then
            assertThat(updatedUser.friendIds).contains(requesterId)
            assertThat(updatedUser.incomingFriendRequestIds).doesNotContain(requesterId)
            assertThat(updatedUser.incomingFriendRequestIds).contains(3L)
            assertThat(updatedUser.updatedAt).isNotNull()
        }
        
        @Test
        @DisplayName("존재하지 않는 친구 요청을 수락하면 변경이 없다")
        fun `존재하지 않는 친구 요청을 수락하면 변경이 없다`() {
            // given
            val user = User(
                id = 1L,
                username = "testuser",
                nickname = "테스트유저",
                userCode = "ABCD1234",
                incomingFriendRequestIds = setOf(2L, 3L)
            )
            val nonExistingRequesterId = 4L
            
            // when
            val updatedUser = user.acceptFriendRequest(nonExistingRequesterId)
            
            // then
            assertThat(updatedUser).isEqualTo(user)
        }
        
        @Test
        @DisplayName("받은 친구 요청을 거절할 수 있다")
        fun `받은 친구 요청을 거절할 수 있다`() {
            // given
            val user = User(
                id = 1L,
                username = "testuser",
                nickname = "테스트유저",
                userCode = "ABCD1234",
                incomingFriendRequestIds = setOf(2L, 3L)
            )
            val requesterId = 2L
            
            // when
            val updatedUser = user.rejectFriendRequest(requesterId)
            
            // then
            assertThat(updatedUser.incomingFriendRequestIds).doesNotContain(requesterId)
            assertThat(updatedUser.incomingFriendRequestIds).contains(3L)
            assertThat(updatedUser.friendIds).doesNotContain(requesterId)
            assertThat(updatedUser.updatedAt).isNotNull()
        }
        
        @Test
        @DisplayName("보낸 친구 요청을 취소할 수 있다")
        fun `보낸 친구 요청을 취소할 수 있다`() {
            // given
            val user = User(
                id = 1L,
                username = "testuser",
                nickname = "테스트유저",
                userCode = "ABCD1234",
                outgoingFriendRequestIds = setOf(2L, 3L)
            )
            val targetUserId = 2L
            
            // when
            val updatedUser = user.cancelFriendRequest(targetUserId)
            
            // then
            assertThat(updatedUser.outgoingFriendRequestIds).doesNotContain(targetUserId)
            assertThat(updatedUser.outgoingFriendRequestIds).contains(3L)
            assertThat(updatedUser.updatedAt).isNotNull()
        }
    }
    
    @Nested
    @DisplayName("계정 관리 시")
    inner class ManageAccount {
    
        @Test
        @DisplayName("계정을 삭제할 수 있다")
        fun `계정을 삭제할 수 있다`() {
            // given
            val user = User(
                id = 1L,
                username = "testuser",
                nickname = "테스트유저",
                userCode = "ABCD1234",
                status = UserStatus.ONLINE
            )
            
            // when
            val deletedUser = user.delete()
            
            // then
            assertThat(deletedUser.isDeleted).isTrue()
            assertThat(deletedUser.status).isEqualTo(UserStatus.OFFLINE)
            assertThat(deletedUser.updatedAt).isNotNull()
        }
    }
}