package com.stark.shoot.adapter.`in`.web.sse

import com.stark.shoot.application.port.`in`.message.mark.MessageReadUseCase
import com.stark.shoot.application.port.`in`.message.mark.command.MarkAllMessagesAsReadCommand
import com.stark.shoot.application.port.`in`.message.mark.command.MarkMessageAsReadCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@DisplayName("MessageReadCountController 단위 테스트")
class MessageReadCountControllerTest {

    private val messageReadUseCase = mock(MessageReadUseCase::class.java)
    private val controller = MessageReadCountController(messageReadUseCase)

    @Test
    @DisplayName("[happy] 채팅방의 모든 메시지를 읽음 처리한다")
    fun `채팅방의 모든 메시지를 읽음 처리한다`() {
        // given
        val roomId = 1L
        val userId = 2L
        val requestId = "request123"

        // when
        val response = controller.markAllMessagesAsRead(roomId, userId, requestId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("모든 메시지가 읽음으로 처리되었습니다.")

        val command = MarkAllMessagesAsReadCommand.of(roomId, userId, requestId)
        verify(messageReadUseCase).markAllMessagesAsRead(command)
    }

    @Test
    @DisplayName("[happy] 채팅방의 모든 메시지를 읽음 처리한다 (requestId 없음)")
    fun `채팅방의 모든 메시지를 읽음 처리한다 (requestId 없음)`() {
        // given
        val roomId = 1L
        val userId = 2L

        // when
        val response = controller.markAllMessagesAsRead(roomId, userId, null)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("모든 메시지가 읽음으로 처리되었습니다.")

        val command = MarkAllMessagesAsReadCommand.of(roomId, userId, null)
        verify(messageReadUseCase).markAllMessagesAsRead(command)
    }

    @Test
    @DisplayName("[happy] 특정 메시지를 읽음 처리한다")
    fun `특정 메시지를 읽음 처리한다`() {
        // given
        val messageId = "message123"
        val userId = 2L

        // when
        val response = controller.markMessageAsRead(messageId, userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("메시지가 읽음으로 처리되었습니다.")

        val command = MarkMessageAsReadCommand.of(messageId, userId)
        verify(messageReadUseCase).markMessageAsRead(command)
    }
}
