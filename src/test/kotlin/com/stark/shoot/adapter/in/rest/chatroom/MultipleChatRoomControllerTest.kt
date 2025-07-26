package com.stark.shoot.adapter.`in`.rest.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.InvitationRequest
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ManageParticipantRequest
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.AddParticipantCommand
import com.stark.shoot.application.port.`in`.chatroom.command.RemoveParticipantCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

@DisplayName("MultipleChatRoomController 단위 테스트")
class MultipleChatRoomControllerTest {

    private val manageChatRoomUseCase = mock(ManageChatRoomUseCase::class.java)
    private val controller = MultipleChatRoomController(manageChatRoomUseCase)

    @Test
    @DisplayName("[happy] 채팅방에 참여자를 추가한다")
    fun `채팅방에 참여자를 추가한다`() {
        // given
        val roomId = 1L
        val userId = 2L
        val request = ManageParticipantRequest(userId)
        val command = AddParticipantCommand.of(roomId, userId)

        `when`(manageChatRoomUseCase.addParticipant(command)).thenReturn(true)

        // when
        val response = controller.addParticipant(roomId, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isTrue()
        assertThat(response.message).isEqualTo("참여자가 추가되었습니다.")

        verify(manageChatRoomUseCase).addParticipant(command)
    }

    @Test
    @DisplayName("[happy] 채팅방에서 참여자를 제거한다")
    fun `채팅방에서 참여자를 제거한다`() {
        // given
        val roomId = 1L
        val userId = 2L
        val request = ManageParticipantRequest(userId)
        val command = RemoveParticipantCommand.of(roomId, userId)

        `when`(manageChatRoomUseCase.removeParticipant(command)).thenReturn(true)

        // when
        val response = controller.removeParticipant(roomId, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isTrue()
        assertThat(response.message).isEqualTo("참여자가 제거되었습니다.")

        verify(manageChatRoomUseCase).removeParticipant(command)
    }

    @Test
    @DisplayName("[happy] 사용자를 채팅방에 초대한다")
    fun `사용자를 채팅방에 초대한다`() {
        // given
        val roomId = 1L
        val userId = 3L
        val request = InvitationRequest(userId)
        val command = AddParticipantCommand.of(roomId, userId)

        `when`(manageChatRoomUseCase.addParticipant(command)).thenReturn(true)

        // when
        val response = controller.inviteParticipant(roomId, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isTrue()
        assertThat(response.message).isEqualTo("사용자를 채팅방에 초대했습니다.")

        verify(manageChatRoomUseCase).addParticipant(command)
    }

    @Test
    @DisplayName("[fail] 채팅방에 참여자 추가에 실패한 경우")
    fun `채팅방에 참여자 추가에 실패한 경우`() {
        // given
        val roomId = 1L
        val userId = 4L
        val request = ManageParticipantRequest(userId)
        val command = AddParticipantCommand.of(roomId, userId)

        `when`(manageChatRoomUseCase.addParticipant(command)).thenReturn(false)

        // when
        val response = controller.addParticipant(roomId, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isFalse()
        assertThat(response.message).isEqualTo("참여자가 추가되었습니다.")

        verify(manageChatRoomUseCase).addParticipant(command)
    }
}
