package com.stark.shoot.adapter.`in`.web.message.pin

import com.stark.shoot.application.port.`in`.message.pin.GetPinnedMessageUseCase
import com.stark.shoot.application.port.`in`.message.pin.command.GetPinnedMessagesCommand
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
import java.time.Instant

@DisplayName("GetPinnedMessageController 단위 테스트")
class GetPinnedMessageControllerTest {

    private val getPinnedMessageUseCase = mock(GetPinnedMessageUseCase::class.java)
    private val controller = GetPinnedMessageController(getPinnedMessageUseCase)

    @Test
    @DisplayName("[happy] 채팅방의 고정된 메시지 목록을 조회한다")
    fun `채팅방의 고정된 메시지 목록을 조회한다`() {
        // given
        val roomId = 1L
        val now = Instant.now()

        val pinnedMessages = listOf(
            createChatMessage(
                messageId = "message123",
                roomId = roomId,
                senderId = 2L,
                content = "첫 번째 고정 메시지",
                isPinned = true,
                pinnedBy = 3L,
                pinnedAt = now
            ),
            createChatMessage(
                messageId = "message456",
                roomId = roomId,
                senderId = 4L,
                content = "두 번째 고정 메시지",
                isPinned = true,
                pinnedBy = 5L,
                pinnedAt = now
            )
        )

        val command = GetPinnedMessagesCommand.of(roomId)
        doReturn(pinnedMessages).`when`(getPinnedMessageUseCase).getPinnedMessages(command)

        // when
        val response = controller.getPinnedMessages(roomId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.roomId).isEqualTo(roomId)
        assertThat(response.data?.pinnedMessages).hasSize(2)
        assertThat(response.data?.pinnedMessages?.get(0)?.messageId).isEqualTo("message123")
        assertThat(response.data?.pinnedMessages?.get(0)?.content).isEqualTo("첫 번째 고정 메시지")
        assertThat(response.data?.pinnedMessages?.get(0)?.senderId).isEqualTo(2L)
        assertThat(response.data?.pinnedMessages?.get(0)?.pinnedBy).isEqualTo(3L)
        assertThat(response.data?.pinnedMessages?.get(1)?.messageId).isEqualTo("message456")
        assertThat(response.data?.pinnedMessages?.get(1)?.content).isEqualTo("두 번째 고정 메시지")
        assertThat(response.data?.pinnedMessages?.get(1)?.senderId).isEqualTo(4L)
        assertThat(response.data?.pinnedMessages?.get(1)?.pinnedBy).isEqualTo(5L)

        verify(getPinnedMessageUseCase).getPinnedMessages(command)
    }

    @Test
    @DisplayName("[happy] 고정된 메시지가 없는 경우 빈 목록을 반환한다")
    fun `고정된 메시지가 없는 경우 빈 목록을 반환한다`() {
        // given
        val roomId = 1L

        val command = GetPinnedMessagesCommand.of(roomId)
        doReturn(emptyList<Any>()).`when`(getPinnedMessageUseCase).getPinnedMessages(command)

        // when
        val response = controller.getPinnedMessages(roomId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.roomId).isEqualTo(roomId)
        assertThat(response.data?.pinnedMessages).isEmpty()

        verify(getPinnedMessageUseCase).getPinnedMessages(command)
    }

    // 테스트용 ChatMessage 객체 생성 헬퍼 메서드
    private fun createChatMessage(
        messageId: String,
        roomId: Long,
        senderId: Long,
        content: String,
        isPinned: Boolean = false,
        pinnedBy: Long? = null,
        pinnedAt: Instant? = null
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
            status = MessageStatus.SAVED,
            createdAt = now,
            updatedAt = now,
            isPinned = isPinned,
            pinnedBy = pinnedBy?.let { UserId.from(it) },
            pinnedAt = pinnedAt
        )
    }
}
