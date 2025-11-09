package com.stark.shoot.application.service.message.pin

import com.stark.shoot.application.port.`in`.message.pin.command.GetPinnedMessagesCommand
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId as ChatRoomIdService
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant
import org.hamcrest.Matchers.hasSize

@DisplayName("고정 메시지 조회 서비스 테스트")
class GetPinnedMessageServiceTest {

    private val messageQueryPort = mock(MessageQueryPort::class.java)

    private val getPinnedMessageService = GetPinnedMessageService(
        messageQueryPort
    )

    @Nested
    @DisplayName("고정 메시지 조회 시")
    inner class GetPinnedMessages {

        @Test
        @DisplayName("[happy] 채팅방의 고정 메시지를 조회할 수 있다")
        fun `채팅방의 고정 메시지를 조회할 수 있다`() {
            // given
            val roomId = ChatRoomId.from(1L)

            val pinnedMessage = ChatMessage(
                id = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9b"),
                roomId = roomId,
                senderId = UserId.from(2L),
                content = MessageContent("고정된 메시지", MessageType.TEXT),
                status = MessageStatus.SENT,
                createdAt = Instant.now(),
                isPinned = true,
                pinnedBy = UserId.from(3L),
                pinnedAt = Instant.now()
            )

            `when`(messageQueryPort.findPinnedMessagesByRoomId(ChatRoomIdService.from(roomId.value), 1)).thenReturn(listOf(pinnedMessage))

            // when
            val command = GetPinnedMessagesCommand(ChatRoomIdService.from(roomId.value))
            val result = getPinnedMessageService.getPinnedMessages(command)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(pinnedMessage)
            assertThat(result[0].isPinned).isTrue()

            verify(messageQueryPort).findPinnedMessagesByRoomId(ChatRoomIdService.from(roomId.value), 1)
        }

        @Test
        @DisplayName("[happy] 고정 메시지가 없는 경우 빈 목록을 반환한다")
        fun `고정 메시지가 없는 경우 빈 목록을 반환한다`() {
            // given
            val roomId = ChatRoomId.from(1L)

            `when`(messageQueryPort.findPinnedMessagesByRoomId(ChatRoomIdService.from(roomId.value), 1)).thenReturn(emptyList())

            // when
            val command = GetPinnedMessagesCommand(ChatRoomIdService.from(roomId.value))
            val result = getPinnedMessageService.getPinnedMessages(command)

            // then
            assertThat(result).isEmpty()

            verify(messageQueryPort).findPinnedMessagesByRoomId(ChatRoomIdService.from(roomId.value), 1)
        }
    }
}
