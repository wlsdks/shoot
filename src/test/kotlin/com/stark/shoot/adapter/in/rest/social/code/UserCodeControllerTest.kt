package com.stark.shoot.adapter.`in`.rest.social.code

import com.stark.shoot.application.port.`in`.user.FindUserUseCase
import com.stark.shoot.application.port.`in`.user.code.ManageUserCodeUseCase
import com.stark.shoot.application.port.`in`.user.code.command.RemoveUserCodeCommand
import com.stark.shoot.application.port.`in`.user.code.command.UpdateUserCodeCommand
import com.stark.shoot.application.port.`in`.user.command.FindUserByIdCommand
import com.stark.shoot.application.port.`in`.user.command.FindUserByCodeCommand
import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.SendFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.SendFriendRequestFromCodeCommand
import com.stark.shoot.adapter.`in`.rest.dto.social.code.UpdateUserCodeRequest
import com.stark.shoot.adapter.`in`.rest.dto.social.code.SendFriendRequestByCodeRequest
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.domain.user.vo.*
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("UserCodeController 단위 테스트")
class UserCodeControllerTest {

    private val findUserUseCase = mock(FindUserUseCase::class.java)
    private val manageUserCodeUseCase = mock(ManageUserCodeUseCase::class.java)
    private val friendRequestUseCase = mock(FriendRequestUseCase::class.java)
    private val controller = UserCodeController(findUserUseCase, manageUserCodeUseCase, friendRequestUseCase)

    @Test
    @DisplayName("[happy] 사용자 ID로 유저 코드를 조회한다")
    fun `사용자 ID로 유저 코드를 조회한다`() {
        // given
        val userId = 1L
        val user = createUser(userId, "testuser", "테스트 유저", "TEST123")
        val command = FindUserByIdCommand.of(userId)

        `when`(findUserUseCase.findById(command)).thenReturn(user)

        // when
        val response = controller.getUserCode(userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(userId.toString())
        assertThat(response.data?.username).isEqualTo("testuser")
        assertThat(response.data?.nickname).isEqualTo("테스트 유저")
        assertThat(response.data?.userCode).isEqualTo("TEST123")
        assertThat(response.message).isEqualTo("유저 코드를 성공적으로 조회했습니다.")

        verify(findUserUseCase).findById(command)
    }

    @Test
    @DisplayName("[fail] 존재하지 않는 사용자 ID로 유저 코드를 조회하면 예외가 발생한다")
    fun `존재하지 않는 사용자 ID로 유저 코드를 조회하면 예외가 발생한다`() {
        // given
        val userId = 999L
        val command = FindUserByIdCommand.of(userId)

        `when`(findUserUseCase.findById(command)).thenReturn(null)

        // when & then
        assertThrows<ResourceNotFoundException> {
            controller.getUserCode(userId)
        }

        verify(findUserUseCase).findById(command)
    }

    @Test
    @DisplayName("[happy] 유저 코드를 업데이트한다")
    fun `유저 코드를 업데이트한다`() {
        // given
        val userId = 1L
        val newCode = "NEWCODE123"
        val request = UpdateUserCodeRequest(userId, newCode)
        val command = UpdateUserCodeCommand.of(request)

        // when
        val response = controller.updateUserCode(request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("유저 코드가 성공적으로 설정되었습니다.")

        verify(manageUserCodeUseCase).updateUserCode(command)
    }

    @Test
    @DisplayName("[happy] 유저 코드로 사용자를 조회한다")
    fun `유저 코드로 사용자를 조회한다`() {
        // given
        val code = "FINDME123"
        val user = createUser(2L, "findme", "찾아주세요", code)
        val command = FindUserByCodeCommand.of(code)

        `when`(findUserUseCase.findByUserCode(command)).thenReturn(user)

        // when
        val response = controller.findUserByCode(code)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo("2")
        assertThat(response.data?.username).isEqualTo("findme")
        assertThat(response.data?.nickname).isEqualTo("찾아주세요")
        assertThat(response.data?.userCode).isEqualTo(code)
        assertThat(response.message).isEqualTo("사용자를 찾았습니다.")

        verify(findUserUseCase).findByUserCode(command)
    }

    @Test
    @DisplayName("[happy] 존재하지 않는 유저 코드로 조회하면 null을 반환한다")
    fun `존재하지 않는 유저 코드로 조회하면 null을 반환한다`() {
        // given
        val code = "NOTEXIST"
        val command = FindUserByCodeCommand.of(code)

        `when`(findUserUseCase.findByUserCode(command)).thenReturn(null)

        // when
        val response = controller.findUserByCode(code)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isNull()
        assertThat(response.message).isEqualTo("해당 코드의 사용자가 없습니다.")

        verify(findUserUseCase).findByUserCode(command)
    }

    @Test
    @DisplayName("[happy] 유저 코드를 삭제(초기화)한다")
    fun `유저 코드를 삭제(초기화)한다`() {
        // given
        val userId = 1L
        val command = RemoveUserCodeCommand.of(userId)

        // when
        val response = controller.removeUserCode(userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("유저 코드가 삭제되었습니다.")

        verify(manageUserCodeUseCase).removeUserCode(command)
    }

    @Test
    @DisplayName("[happy] 유저 코드로 친구 요청을 보낸다")
    fun `유저 코드로 친구 요청을 보낸다`() {
        // given
        val userId = 1L
        val targetCode = "FRIEND123"
        val targetUser = createUser(3L, "friend", "친구", targetCode)
        val request = SendFriendRequestByCodeRequest(userId, targetCode)
        val findCommand = FindUserByCodeCommand.of(targetCode)
        val requestCommand = SendFriendRequestFromCodeCommand.of(request, UserId.from(3L))

        `when`(findUserUseCase.findByUserCode(findCommand)).thenReturn(targetUser)

        // when
        val response = controller.sendFriendRequestByCode(request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("친구 요청을 보냈습니다.")

        verify(findUserUseCase).findByUserCode(findCommand)
        verify(friendRequestUseCase).sendFriendRequestFromUserCode(requestCommand)
    }

    @Test
    @DisplayName("[fail] 존재하지 않는 유저 코드로 친구 요청을 보내면 예외가 발생한다")
    fun `존재하지 않는 유저 코드로 친구 요청을 보내면 예외가 발생한다`() {
        // given
        val userId = 1L
        val targetCode = "NOTEXIST"
        val request = SendFriendRequestByCodeRequest(userId, targetCode)
        val findCommand = FindUserByCodeCommand.of(targetCode)

        `when`(findUserUseCase.findByUserCode(findCommand)).thenReturn(null)

        // when & then
        assertThrows<ResourceNotFoundException> {
            controller.sendFriendRequestByCode(request)
        }

        verify(findUserUseCase).findByUserCode(findCommand)
        // No need to verify that sendFriendRequest is not called since an exception is thrown
    }

    // 테스트용 User 객체 생성 헬퍼 메서드
    private fun createUser(
        id: Long,
        username: String,
        nickname: String,
        userCode: String,
        status: UserStatus = UserStatus.ONLINE,
        bio: String? = null,
        profileImageUrl: String? = null,
        backgroundImageUrl: String? = null
    ): User {
        return User(
            id = UserId.from(id),
            username = Username.from(username),
            nickname = Nickname.from(nickname),
            status = status,
            userCode = UserCode.from(userCode),
            bio = bio?.let { UserBio.from(it) },
            profileImageUrl = profileImageUrl?.let { ProfileImageUrl.from(it) },
            backgroundImageUrl = backgroundImageUrl?.let { BackgroundImageUrl.from(it) },
            lastSeenAt = Instant.now()
        )
    }
}
