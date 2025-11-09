package com.stark.shoot.adapter.`in`.rest.message

import com.stark.shoot.adapter.`in`.rest.dto.message.DeleteMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.EditMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageContentResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.DeleteMessageUseCase
import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.DeleteMessageCommand
import com.stark.shoot.application.port.`in`.message.command.EditMessageCommand
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("MessageController 단위 테스트")
class MessageControllerTest {

    private val editMessageUseCase = mock(EditMessageUseCase::class.java)
    private val deleteMessageUseCase = mock(DeleteMessageUseCase::class.java)
    private val chatMessageMapper = mock(ChatMessageMapper::class.java)
    private val controller = MessageController(editMessageUseCase, deleteMessageUseCase, chatMessageMapper)

    @Test
    @DisplayName("[happy] 메시지 편집 요청을 처리하고 수정된 메시지를 반환한다")
    fun `메시지 편집 요청을 처리하고 수정된 메시지를 반환한다`() {
        // given
        val messageId = "message123"
        val newContent = "수정된 메시지 내용"
        val request = EditMessageRequest(messageId, newContent, 2L)
        val command = EditMessageCommand.of(messageId, newContent, 2L)

        val updatedMessage = createChatMessage(messageId, newContent, true)
        val responseDto = createMessageResponseDto(messageId, newContent, true)

        `when`(editMessageUseCase.editMessage(command)).thenReturn(updatedMessage)
        `when`(chatMessageMapper.toDto(updatedMessage)).thenReturn(responseDto)

        // when
        val response = controller.editMessage(request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(responseDto)
        assertThat(response.message).isEqualTo("메시지가 수정되었습니다.")

        verify(editMessageUseCase).editMessage(command)
        verify(chatMessageMapper).toDto(updatedMessage)
    }

    @Test
    @DisplayName("[happy] 메시지 삭제 요청을 처리하고 삭제된 메시지를 반환한다")
    fun `메시지 삭제 요청을 처리하고 삭제된 메시지를 반환한다`() {
        // given
        val messageId = "message123"
        val request = DeleteMessageRequest(messageId, 2L)
        val command = DeleteMessageCommand.of(messageId, 2L)

        val deletedMessage = createChatMessage(messageId, "원본 메시지", false, true)
        val responseDto = createMessageResponseDto(messageId, "원본 메시지", false, true)

        `when`(deleteMessageUseCase.deleteMessage(command)).thenReturn(deletedMessage)
        `when`(chatMessageMapper.toDto(deletedMessage)).thenReturn(responseDto)

        // when
        val response = controller.deleteMessage(request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(responseDto)
        assertThat(response.message).isEqualTo("메시지가 삭제되었습니다.")

        verify(deleteMessageUseCase).deleteMessage(command)
        verify(chatMessageMapper).toDto(deletedMessage)
    }

    // 테스트용 ChatMessage 객체 생성 헬퍼 메서드
    private fun createChatMessage(
        messageId: String, 
        content: String, 
        isEdited: Boolean = false,
        isDeleted: Boolean = false
    ): ChatMessage {
        return ChatMessage(
            id = MessageId.from(messageId),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent(
                text = content,
                type = MessageType.TEXT,
                isEdited = isEdited,
                isDeleted = isDeleted
            ),
            status = MessageStatus.SENT,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    // 테스트용 MessageResponseDto 객체 생성 헬퍼 메서드
    private fun createMessageResponseDto(
        messageId: String, 
        content: String, 
        isEdited: Boolean = false,
        isDeleted: Boolean = false
    ): MessageResponseDto {
        return MessageResponseDto(
            id = messageId,
            roomId = 1L,
            senderId = 2L,
            content = MessageContentResponseDto(
                text = content,
                type = MessageType.TEXT,
                isEdited = isEdited,
                isDeleted = isDeleted,
                attachments = emptyList(),
                urlPreview = null
            ),
            status = MessageStatus.SENT,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
