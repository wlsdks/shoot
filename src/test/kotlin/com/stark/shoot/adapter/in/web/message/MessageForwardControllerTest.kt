package com.stark.shoot.adapter.`in`.web.message

import com.stark.shoot.application.port.`in`.message.ForwardMessageToUserUseCase
import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.ForwardMessageCommand
import com.stark.shoot.application.port.`in`.message.command.ForwardMessageToUserCommand
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("MessageForwardController 단위 테스트")
class MessageForwardControllerTest {

    private val forwardMessageUseCase = mock(ForwardMessageUseCase::class.java)
    private val forwardMessageToUserUseCase = mock(ForwardMessageToUserUseCase::class.java)
    private val controller = MessageForwardController(forwardMessageUseCase, forwardMessageToUserUseCase)

    @Test
    @DisplayName("[happy] 메시지를 다른 채팅방으로 전달하고 전달된 메시지를 반환한다")
    fun `메시지를 다른 채팅방으로 전달하고 전달된 메시지를 반환한다`() {
        // given
        val originalMessageId = "message123"
        val targetRoomId = 2L
        val forwardingUserId = 1L
        val command = ForwardMessageCommand.of(originalMessageId, targetRoomId, forwardingUserId)

        val forwardedMessage = createChatMessage(
            messageId = "forwarded123",
            roomId = targetRoomId,
            senderId = forwardingUserId,
            content = "전달된 메시지 내용"
        )

        `when`(forwardMessageUseCase.forwardMessage(command)).thenReturn(forwardedMessage)

        // when
        val response = controller.forwardMessage(originalMessageId, targetRoomId, forwardingUserId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.status).isEqualTo(MessageStatus.SAVED.name)
        assertThat(response.data?.content).isEqualTo("전달된 메시지 내용")
        assertThat(response.message).isEqualTo("메시지가 전달되었습니다.")

        verify(forwardMessageUseCase).forwardMessage(command)
    }

    @Test
    @DisplayName("[happy] 메시지를 특정 사용자에게 전달하고 전달된 메시지를 반환한다")
    fun `메시지를 특정 사용자에게 전달하고 전달된 메시지를 반환한다`() {
        // given
        val originalMessageId = "message123"
        val targetUserId = 2L
        val forwardingUserId = 1L
        val command = ForwardMessageToUserCommand.of(originalMessageId, targetUserId, forwardingUserId)

        val forwardedMessage = createChatMessage(
            messageId = "forwarded123",
            roomId = 3L, // 새로 생성된 또는 기존 1:1 채팅방 ID
            senderId = forwardingUserId,
            content = "사용자에게 전달된 메시지 내용"
        )

        `when`(forwardMessageToUserUseCase.forwardMessageToUser(command)).thenReturn(forwardedMessage)

        // when
        val response = controller.forwardMessageToUser(originalMessageId, targetUserId, forwardingUserId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.status).isEqualTo(MessageStatus.SAVED.name)
        assertThat(response.data?.content).isEqualTo("사용자에게 전달된 메시지 내용")
        assertThat(response.message).isEqualTo("메시지가 사용자에게 전달되었습니다.")

        verify(forwardMessageToUserUseCase).forwardMessageToUser(command)
    }

    // 테스트용 ChatMessage 객체 생성 헬퍼 메서드
    private fun createChatMessage(
        messageId: String,
        roomId: Long,
        senderId: Long,
        content: String
    ): ChatMessage {
        return ChatMessage(
            id = MessageId.from(messageId),
            roomId = ChatRoomId.from(roomId),
            senderId = UserId.from(senderId),
            content = MessageContent(
                text = content,
                type = com.stark.shoot.domain.chat.message.type.MessageType.TEXT
            ),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )
    }
}
