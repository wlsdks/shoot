package com.stark.shoot.adapter.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.MessageQueryMongoAdapter
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Instant
import java.util.*
import org.hamcrest.Matchers.hasSize

@DisplayName("메시지 조회 MongoDB 어댑터 테스트")
class MessageQueryMongoAdapterTest {

    private val chatMessageRepository = mock(ChatMessageMongoRepository::class.java)
    private val mongoTemplate = mock(org.springframework.data.mongodb.core.MongoTemplate::class.java)
    private val chatMessageMapper = mock(ChatMessageMapper::class.java)
    private val adapter = MessageQueryMongoAdapter(chatMessageRepository, mongoTemplate, chatMessageMapper)

    private fun createChatMessage(id: String = "123456789012345678901234"): ChatMessage {
        return ChatMessage(
            id = MessageId.from(id),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent("Test message", MessageType.TEXT),
            status = MessageStatus.SENT,
            createdAt = Instant.now()
        )
    }

    private fun createChatMessageDocument(id: String = "123456789012345678901234"): ChatMessageDocument {
        val document = mock(ChatMessageDocument::class.java)
        `when`(document.id).thenReturn(ObjectId(id))
        return document
    }

    @Test
    @DisplayName("[happy] ID로 메시지를 조회할 수 있다")
    fun `ID로 메시지를 조회할 수 있다`() {
        // given
        val messageId = MessageId.from("123456789012345678901234")
        val objectId = ObjectId("123456789012345678901234")
        val document = createChatMessageDocument()
        val message = createChatMessage()

        `when`(chatMessageRepository.findById(objectId)).thenReturn(Optional.of(document))
        `when`(chatMessageMapper.toDomain(document)).thenReturn(message)

        // when
        val result = adapter.findById(messageId)

        // then
        assertThat(result).isEqualTo(message)
        verify(chatMessageRepository).findById(objectId)
        verify(chatMessageMapper).toDomain(document)
    }

    @Test
    @DisplayName("[happy] 채팅방 ID로 메시지를 조회할 수 있다")
    fun `채팅방 ID로 메시지를 조회할 수 있다`() {
        // given
        val roomId = ChatRoomId.from(1L)
        val limit = 10
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id"))

        val documents = listOf(
            createChatMessageDocument("123456789012345678901234"),
            createChatMessageDocument("123456789012345678901235")
        )

        val messages = listOf(
            createChatMessage("123456789012345678901234"),
            createChatMessage("123456789012345678901235")
        )

        `when`(chatMessageRepository.findByRoomId(roomId.value, pageable)).thenReturn(documents)
        `when`(chatMessageMapper.toDomain(documents[0])).thenReturn(messages[0])
        `when`(chatMessageMapper.toDomain(documents[1])).thenReturn(messages[1])

        // when
        val result = adapter.findByRoomId(roomId, limit)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(messages)
        verify(chatMessageRepository).findByRoomId(roomId.value, pageable)
        verify(chatMessageMapper).toDomain(documents[0])
        verify(chatMessageMapper).toDomain(documents[1])
    }

    @Test
    @DisplayName("[happy] 채팅방 ID와 이전 메시지 ID로 이전 메시지를 조회할 수 있다")
    fun `채팅방 ID와 이전 메시지 ID로 이전 메시지를 조회할 수 있다`() {
        // given
        val roomId = ChatRoomId.from(1L)
        val beforeMessageId = MessageId.from("123456789012345678901234")
        val limit = 10
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id"))

        val documents = listOf(
            createChatMessageDocument("123456789012345678901235"),
            createChatMessageDocument("123456789012345678901236")
        )

        val messages = listOf(
            createChatMessage("123456789012345678901235"),
            createChatMessage("123456789012345678901236")
        )

        `when`(chatMessageRepository.findByRoomIdAndIdBefore(
            roomId.value, 
            ObjectId("123456789012345678901234"), 
            pageable
        )).thenReturn(documents)

        `when`(chatMessageMapper.toDomain(documents[0])).thenReturn(messages[0])
        `when`(chatMessageMapper.toDomain(documents[1])).thenReturn(messages[1])

        // when
        val result = adapter.findByRoomIdAndBeforeId(roomId, beforeMessageId, limit)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(messages)
        verify(chatMessageRepository).findByRoomIdAndIdBefore(
            roomId.value, 
            ObjectId("123456789012345678901234"), 
            pageable
        )
        verify(chatMessageMapper).toDomain(documents[0])
        verify(chatMessageMapper).toDomain(documents[1])
    }

    @Test
    @DisplayName("[happy] 채팅방 ID와 이후 메시지 ID로 이후 메시지를 조회할 수 있다")
    fun `채팅방 ID와 이후 메시지 ID로 이후 메시지를 조회할 수 있다`() {
        // given
        val roomId = ChatRoomId.from(1L)
        val afterMessageId = MessageId.from("123456789012345678901234")
        val limit = 10
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "_id"))

        val documents = listOf(
            createChatMessageDocument("123456789012345678901235"),
            createChatMessageDocument("123456789012345678901236")
        )

        val messages = listOf(
            createChatMessage("123456789012345678901235"),
            createChatMessage("123456789012345678901236")
        )

        `when`(chatMessageRepository.findByRoomIdAndIdAfter(
            roomId.value, 
            ObjectId("123456789012345678901234"), 
            pageable
        )).thenReturn(documents)

        `when`(chatMessageMapper.toDomain(documents[0])).thenReturn(messages[0])
        `when`(chatMessageMapper.toDomain(documents[1])).thenReturn(messages[1])

        // when
        val result = adapter.findByRoomIdAndAfterId(roomId, afterMessageId, limit)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(messages)
        verify(chatMessageRepository).findByRoomIdAndIdAfter(
            roomId.value, 
            ObjectId("123456789012345678901234"), 
            pageable
        )
        verify(chatMessageMapper).toDomain(documents[0])
        verify(chatMessageMapper).toDomain(documents[1])
    }

    @Test
    @DisplayName("[happy] 채팅방 ID와 사용자 ID로 읽지 않은 메시지를 조회할 수 있다")
    fun `채팅방 ID와 사용자 ID로 읽지 않은 메시지를 조회할 수 있다`() {
        // given
        val roomId = ChatRoomId.from(1L)
        val userId = UserId.from(2L)
        val limit = 10
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))

        val documents = listOf(
            createChatMessageDocument("123456789012345678901234"),
            createChatMessageDocument("123456789012345678901235")
        )

        val messages = listOf(
            createChatMessage("123456789012345678901234"),
            createChatMessage("123456789012345678901235")
        )

        `when`(chatMessageRepository.findUnreadMessages(roomId.value, userId.value, pageable)).thenReturn(documents)
        `when`(chatMessageMapper.toDomain(documents[0])).thenReturn(messages[0])
        `when`(chatMessageMapper.toDomain(documents[1])).thenReturn(messages[1])

        // when
        val result = adapter.findUnreadByRoomId(roomId, userId, limit)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(messages)
        verify(chatMessageRepository).findUnreadMessages(roomId.value, userId.value, pageable)
        verify(chatMessageMapper).toDomain(documents[0])
        verify(chatMessageMapper).toDomain(documents[1])
    }

    @Test
    @DisplayName("[happy] 채팅방 ID로 고정된 메시지를 조회할 수 있다")
    fun `채팅방 ID로 고정된 메시지를 조회할 수 있다`() {
        // given
        val roomId = ChatRoomId.from(1L)
        val limit = 10
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))

        val documents = listOf(
            createChatMessageDocument("123456789012345678901234"),
            createChatMessageDocument("123456789012345678901235")
        )

        val messages = listOf(
            createChatMessage("123456789012345678901234"),
            createChatMessage("123456789012345678901235")
        )

        `when`(chatMessageRepository.findPinnedMessagesByRoomId(roomId.value, pageable)).thenReturn(documents)
        `when`(chatMessageMapper.toDomain(documents[0])).thenReturn(messages[0])
        `when`(chatMessageMapper.toDomain(documents[1])).thenReturn(messages[1])

        // when
        val result = adapter.findPinnedMessagesByRoomId(roomId, limit)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(messages)
        verify(chatMessageRepository).findPinnedMessagesByRoomId(roomId.value, pageable)
        verify(chatMessageMapper).toDomain(documents[0])
        verify(chatMessageMapper).toDomain(documents[1])
    }

    @Test
    @DisplayName("[happy] 채팅방 ID로 메시지를 Flow로 조회할 수 있다")
    fun `채팅방 ID로 메시지를 Flow로 조회할 수 있다`() {
        runBlocking {
            // given
            val roomId = ChatRoomId.from(1L)
            val limit = 10

            val messages = listOf(
                createChatMessage("123456789012345678901234"),
                createChatMessage("123456789012345678901235")
            )

            // Create a spy of the adapter to mock the findByRoomId method
            val adapterSpy = spy(adapter)
            doReturn(messages).`when`(adapterSpy).findByRoomId(roomId, limit)

            // when
            val result = adapterSpy.findByRoomIdFlow(roomId, limit).toList()

            // then
            assertThat(result).hasSize(2)
            assertThat(result).isEqualTo(messages)
            verify(adapterSpy).findByRoomId(roomId, limit)
        }
    }

    @Test
    @DisplayName("[happy] 채팅방 ID와 이전 메시지 ID로 이전 메시지를 Flow로 조회할 수 있다")
    fun `채팅방 ID와 이전 메시지 ID로 이전 메시지를 Flow로 조회할 수 있다`() {
        runBlocking {
            // given
            val roomId = ChatRoomId.from(1L)
            val beforeMessageId = MessageId.from("123456789012345678901234")
            val limit = 10

            val messages = listOf(
                createChatMessage("123456789012345678901235"),
                createChatMessage("123456789012345678901236")
            )

            // Create a spy of the adapter to mock the findByRoomIdAndBeforeId method
            val adapterSpy = spy(adapter)
            doReturn(messages).`when`(adapterSpy).findByRoomIdAndBeforeId(roomId, beforeMessageId, limit)

            // when
            val result = adapterSpy.findByRoomIdAndBeforeIdFlow(roomId, beforeMessageId, limit).toList()

            // then
            assertThat(result).hasSize(2)
            assertThat(result).isEqualTo(messages)
            verify(adapterSpy).findByRoomIdAndBeforeId(roomId, beforeMessageId, limit)
        }
    }

    @Test
    @DisplayName("[happy] 채팅방 ID와 이후 메시지 ID로 이후 메시지를 Flow로 조회할 수 있다")
    fun `채팅방 ID와 이후 메시지 ID로 이후 메시지를 Flow로 조회할 수 있다`() {
        runBlocking {
            // given
            val roomId = ChatRoomId.from(1L)
            val afterMessageId = MessageId.from("123456789012345678901234")
            val limit = 10

            val messages = listOf(
                createChatMessage("123456789012345678901235"),
                createChatMessage("123456789012345678901236")
            )

            // Create a spy of the adapter to mock the findByRoomIdAndAfterId method
            val adapterSpy = spy(adapter)
            doReturn(messages).`when`(adapterSpy).findByRoomIdAndAfterId(roomId, afterMessageId, limit)

            // when
            val result = adapterSpy.findByRoomIdAndAfterIdFlow(roomId, afterMessageId, limit).toList()

            // then
            assertThat(result).hasSize(2)
            assertThat(result).isEqualTo(messages)
            verify(adapterSpy).findByRoomIdAndAfterId(roomId, afterMessageId, limit)
        }
    }
}
