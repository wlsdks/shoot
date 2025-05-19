package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("메시지 사용자 전달 서비스 테스트")
class ForwardMessageToUserServiceTest {

    private val createChatRoomUseCase = mock(CreateChatRoomUseCase::class.java)
    private val forwardMessageUseCase = mock(ForwardMessageUseCase::class.java)

    private val forwardMessageToUserService = ForwardMessageToUserService(
        createChatRoomUseCase,
        forwardMessageUseCase
    )

    @Nested
    @DisplayName("메시지를 사용자에게 전달 시")
    inner class ForwardMessageToUser {

        @Test
        @DisplayName("메시지를 특정 사용자에게 전달할 수 있다")
        fun `메시지를 특정 사용자에게 전달할 수 있다`() {
            // given
            val originalMessageId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val targetUserId = 2L
            val forwardingUserId = 1L
            val roomId = 3L

            val chatRoom = ChatRoom(
                id = roomId,
                title = "1:1 채팅",
                type = ChatRoomType.INDIVIDUAL,
                participants = mutableSetOf(forwardingUserId, targetUserId)
            )

            val forwardedMessage = ChatMessage(
                id = "6f9f1b9b9c9d1b9b9c9d1b9c",
                roomId = roomId,
                senderId = forwardingUserId,
                content = MessageContent("전달된 메시지", MessageType.TEXT),
                status = MessageStatus.SAVED,
                createdAt = Instant.now()
            )

            `when`(createChatRoomUseCase.createDirectChat(forwardingUserId, targetUserId)).thenReturn(chatRoom)
            `when`(forwardMessageUseCase.forwardMessage(originalMessageId, roomId, forwardingUserId)).thenReturn(forwardedMessage)

            // when
            val result = forwardMessageToUserService.forwardMessageToUser(originalMessageId, targetUserId, forwardingUserId)

            // then
            assertThat(result).isEqualTo(forwardedMessage)
            verify(createChatRoomUseCase).createDirectChat(forwardingUserId, targetUserId)
            verify(forwardMessageUseCase).forwardMessage(originalMessageId, roomId, forwardingUserId)
        }

        @Test
        @DisplayName("채팅방 ID가 null이면 예외가 발생한다")
        fun `채팅방 ID가 null이면 예외가 발생한다`() {
            // given
            val originalMessageId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val targetUserId = 2L
            val forwardingUserId = 1L

            val chatRoom = ChatRoom(
                id = null,
                title = "1:1 채팅",
                type = ChatRoomType.INDIVIDUAL,
                participants = mutableSetOf(forwardingUserId, targetUserId)
            )

            `when`(createChatRoomUseCase.createDirectChat(forwardingUserId, targetUserId)).thenReturn(chatRoom)

            // when & then
            assertThrows<IllegalStateException> {
                forwardMessageToUserService.forwardMessageToUser(originalMessageId, targetUserId, forwardingUserId)
            }

            verify(createChatRoomUseCase).createDirectChat(forwardingUserId, targetUserId)
            verifyNoInteractions(forwardMessageUseCase)
        }
    }
}
