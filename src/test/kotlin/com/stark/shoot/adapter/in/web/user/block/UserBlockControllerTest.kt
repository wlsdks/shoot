package com.stark.shoot.adapter.`in`.web.user.block

import com.stark.shoot.application.port.`in`.user.block.UserBlockUseCase
import com.stark.shoot.application.port.`in`.user.block.command.BlockUserCommand
import com.stark.shoot.application.port.`in`.user.block.command.UnblockUserCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.core.Authentication

@DisplayName("UserBlockController 단위 테스트")
class UserBlockControllerTest {

    private val userBlockUseCase = mock(UserBlockUseCase::class.java)
    private val authentication = mock(Authentication::class.java)
    private val controller = UserBlockController(userBlockUseCase)

    @Test
    @DisplayName("[happy] 사용자를 차단한다")
    fun `사용자를 차단한다`() {
        // given
        val userId = 1L
        val targetId = 2L

        `when`(authentication.name).thenReturn(userId.toString())

        // when
        val response = controller.blockUser(targetId, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isTrue()
        assertThat(response.message).isEqualTo("사용자를 차단했습니다.")

        verify(authentication).name
        val command = BlockUserCommand.of(userId, targetId)
        verify(userBlockUseCase).blockUser(command)
    }

    @Test
    @DisplayName("[happy] 사용자 차단을 해제한다")
    fun `사용자 차단을 해제한다`() {
        // given
        val userId = 1L
        val targetId = 2L

        `when`(authentication.name).thenReturn(userId.toString())

        // when
        val response = controller.unblockUser(targetId, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isTrue()
        assertThat(response.message).isEqualTo("차단을 해제했습니다.")

        verify(authentication).name
        val command = UnblockUserCommand.of(userId, targetId)
        verify(userBlockUseCase).unblockUser(command)
    }
}
