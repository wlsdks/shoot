package com.stark.shoot.adapter.`in`.rest.message.pin

import com.stark.shoot.application.port.`in`.message.pin.MessagePinUseCase
import com.stark.shoot.application.port.`in`.message.pin.command.PinMessageCommand
import com.stark.shoot.application.port.`in`.message.pin.command.UnpinMessageCommand
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.core.Authentication
import java.time.Instant

@DisplayName("MessagePinController 단위 테스트")
class MessagePinControllerTest {

    private val messagePinUseCase = mock(MessagePinUseCase::class.java)
    private val authentication = mock(Authentication::class.java)
    private val controller = MessagePinController(messagePinUseCase)

    @Test
    @DisplayName("[happy] 메시지 고정 요청을 처리하고 고정된 메시지를 반환한다")
    fun `메시지 고정 요청을 처리하고 고정된 메시지를 반환한다`() {
        // given
        val messageId = "message123"
        val userId = 1L

        `when`(authentication.name).thenReturn(userId.toString())

        val pinnedMessage = createChatMessage(
            messageId = messageId,
            roomId = 1L,
            senderId = 2L,
            content = "중요한 메시지",
            isPinned = true,
            pinnedBy = userId
        )

        val pinCommand = PinMessageCommand.of(messageId, authentication)
        `when`(messagePinUseCase.pinMessage(pinCommand))
            .thenReturn(pinnedMessage)

        // when
        val response = controller.pinMessage(messageId, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.messageId).isEqualTo(messageId)
        assertThat(response.data?.isPinned).isTrue()
        assertThat(response.data?.pinnedBy).isEqualTo(userId)
        assertThat(response.message).isEqualTo("메시지가 고정되었습니다.")

        verify(messagePinUseCase).pinMessage(pinCommand)
    }

    @Test
    @DisplayName("[happy] 메시지 고정 해제 요청을 처리하고 고정 해제된 메시지를 반환한다")
    fun `메시지 고정 해제 요청을 처리하고 고정 해제된 메시지를 반환한다`() {
        // given
        val messageId = "message123"
        val userId = 1L

        `when`(authentication.name).thenReturn(userId.toString())

        val unpinnedMessage = createChatMessage(
            messageId = messageId,
            roomId = 1L,
            senderId = 2L,
            content = "일반 메시지",
            isPinned = false,
            pinnedBy = null
        )

        val unpinCommand = UnpinMessageCommand.of(messageId, authentication)
        `when`(messagePinUseCase.unpinMessage(unpinCommand))
            .thenReturn(unpinnedMessage)

        // when
        val response = controller.unpinMessage(messageId, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.messageId).isEqualTo(messageId)
        assertThat(response.data?.isPinned).isFalse()
        assertThat(response.data?.pinnedBy).isNull()
        assertThat(response.message).isEqualTo("메시지 고정이 해제되었습니다.")

        verify(messagePinUseCase).unpinMessage(unpinCommand)
    }

    // 테스트용 ChatMessage 객체 생성 헬퍼 메서드
    private fun createChatMessage(
        messageId: String,
        roomId: Long,
        senderId: Long,
        content: String,
        isPinned: Boolean = false,
        pinnedBy: Long? = null
    ): ChatMessage {
        val now = Instant.now()
        return ChatMessage(
            id = MessageId.from(messageId),
            roomId = ChatRoomId.from(roomId),
            senderId = UserId.from(senderId),
            content = MessageContent(
                text = content,
                type = MessageType.TEXT
            ),
            status = MessageStatus.SENT,
            createdAt = now,
            updatedAt = now,
            isPinned = isPinned,
            pinnedBy = pinnedBy?.let { UserId.from(it) },
            pinnedAt = if (isPinned) now else null
        )
    }
}
