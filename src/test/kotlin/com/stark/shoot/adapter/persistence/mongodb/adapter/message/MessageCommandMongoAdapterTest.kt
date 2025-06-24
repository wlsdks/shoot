package com.stark.shoot.adapter.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.MessageCommandMongoAdapter
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
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

@DisplayName("메시지 명령 MongoDB 어댑터 테스트")
class MessageCommandMongoAdapterTest {

    private val chatMessageRepository = mock(ChatMessageMongoRepository::class.java)
    private val chatMessageMapper = mock(ChatMessageMapper::class.java)
    private val adapter = MessageCommandMongoAdapter(chatMessageRepository, chatMessageMapper)

    private fun createChatMessage(id: String = "test-message-id"): ChatMessage {
        return ChatMessage(
            id = MessageId.from(id),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent("Test message", MessageType.TEXT),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )
    }

    private fun createChatMessageDocument(id: String = "123456789012345678901234"): ChatMessageDocument {
        val document = mock(ChatMessageDocument::class.java)
        `when`(document.id).thenReturn(org.bson.types.ObjectId(id))
        return document
    }

    @Test
    @DisplayName("[happy] 단일 메시지를 저장할 수 있다")
    fun `단일 메시지를 저장할 수 있다`() {
        // given
        val message = createChatMessage()
        val document = createChatMessageDocument()
        val savedDocument = createChatMessageDocument()
        val savedMessage = createChatMessage()

        `when`(chatMessageMapper.toDocument(message)).thenReturn(document)
        `when`(chatMessageRepository.save(document)).thenReturn(savedDocument)
        `when`(chatMessageMapper.toDomain(savedDocument)).thenReturn(savedMessage)

        // when
        val result = adapter.save(message)

        // then
        assertThat(result).isEqualTo(savedMessage)
        verify(chatMessageMapper).toDocument(message)
        verify(chatMessageRepository).save(document)
        verify(chatMessageMapper).toDomain(savedDocument)
    }

    @Test
    @DisplayName("[happy] 여러 메시지를 한 번에 저장할 수 있다")
    fun `여러 메시지를 한 번에 저장할 수 있다`() {
        // given
        val messages = listOf(
            createChatMessage("111111111111111111111111"),
            createChatMessage("222222222222222222222222"),
            createChatMessage("333333333333333333333333")
        )

        val documents = listOf(
            createChatMessageDocument("111111111111111111111111"),
            createChatMessageDocument("222222222222222222222222"),
            createChatMessageDocument("333333333333333333333333")
        )

        val savedDocuments = listOf(
            createChatMessageDocument("111111111111111111111111"),
            createChatMessageDocument("222222222222222222222222"),
            createChatMessageDocument("333333333333333333333333")
        )

        val savedMessages = listOf(
            createChatMessage("111111111111111111111111"),
            createChatMessage("222222222222222222222222"),
            createChatMessage("333333333333333333333333")
        )

        // Setup mocks for each message
        for (i in messages.indices) {
            `when`(chatMessageMapper.toDocument(messages[i])).thenReturn(documents[i])
            `when`(chatMessageMapper.toDomain(savedDocuments[i])).thenReturn(savedMessages[i])
        }

        `when`(chatMessageRepository.saveAll(documents)).thenReturn(savedDocuments)

        // when
        val result = adapter.saveAll(messages)

        // then
        assertThat(result).hasSize(3)
        assertThat(result).isEqualTo(savedMessages)

        // Verify each message was mapped to a document
        for (i in messages.indices) {
            verify(chatMessageMapper).toDocument(messages[i])
        }

        // Verify saveAll was called with the documents
        verify(chatMessageRepository).saveAll(documents)

        // Verify each saved document was mapped back to a domain object
        for (i in savedDocuments.indices) {
            verify(chatMessageMapper).toDomain(savedDocuments[i])
        }
    }
}
