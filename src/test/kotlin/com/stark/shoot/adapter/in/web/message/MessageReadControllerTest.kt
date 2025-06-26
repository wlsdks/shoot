package com.stark.shoot.adapter.`in`.web.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.application.port.`in`.message.GetMessagesUseCase
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("MessageReadController 단위 테스트")
class MessageReadControllerTest {

    @Test
    @DisplayName("[happy] 채팅방의 메시지를 페이지네이션하여 조회한다")
    fun `채팅방의 메시지를 페이지네이션하여 조회한다`() {
        // given
        val roomId = 1L
        val lastMessageId = "message123"
        val limit = 20

        val messageResponses = listOf(
            createMessageResponseDto("message124", roomId, 2L, "첫 번째 메시지"),
            createMessageResponseDto("message125", roomId, 3L, "두 번째 메시지"),
            createMessageResponseDto("message126", roomId, 2L, "세 번째 메시지")
        )

        val mockUseCase = mock(GetMessagesUseCase::class.java)
        doReturn(messageResponses).`when`(mockUseCase).getMessages(
            ChatRoomId.from(roomId),
            MessageId.from(lastMessageId),
            limit
        )

        val controller = MessageReadController(mockUseCase)

        // when
        val response = controller.getMessages(roomId, lastMessageId, limit)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(3)
        assertThat(response.data?.get(0)?.id).isEqualTo("message124")
        assertThat(response.data?.get(1)?.id).isEqualTo("message125")
        assertThat(response.data?.get(2)?.id).isEqualTo("message126")
    }

    // 테스트용 MessageResponseDto 객체 생성 헬퍼 메서드
    private fun createMessageResponseDto(
        messageId: String,
        roomId: Long,
        senderId: Long,
        content: String
    ): MessageResponseDto {
        return MessageResponseDto(
            id = messageId,
            roomId = roomId,
            senderId = senderId,
            content = MessageContentResponseDto(
                text = content,
                type = MessageType.TEXT,
                isEdited = false,
                isDeleted = false
            ),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )
    }
}
