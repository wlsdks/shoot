package com.stark.shoot.application.service.message.bookmark

import com.stark.shoot.application.port.out.message.BookmarkMessagePort
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("메시지 북마크 서비스 테스트")
class MessageBookmarkServiceTest {

    @Test
    fun `존재하지 않는 메시지를 북마크하면 예외가 발생한다`() {
        val bookmarkPort = mock(BookmarkMessagePort::class.java)
        val loadMessagePort = mock(LoadMessagePort::class.java)
        val service = MessageBookmarkService(bookmarkPort, loadMessagePort)

        val messageId = "1"
        val userId = 1L

        `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(null)

        assertThrows<ResourceNotFoundException> { service.bookmarkMessage(messageId, userId) }
        verify(loadMessagePort).findById(messageId.toObjectId())
        verifyNoInteractions(bookmarkPort)
    }

    @Test
    fun `이미 북마크된 메시지를 다시 북마크하면 예외가 발생한다`() {
        val bookmarkPort = mock(BookmarkMessagePort::class.java)
        val loadMessagePort = mock(LoadMessagePort::class.java)
        val service = MessageBookmarkService(bookmarkPort, loadMessagePort)

        val messageId = "1"
        val userId = 1L

        `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(createMessage(messageId))
        `when`(bookmarkPort.exists(messageId, userId)).thenReturn(true)

        assertThrows<IllegalArgumentException> { service.bookmarkMessage(messageId, userId) }

        verify(bookmarkPort).exists(messageId, userId)
        verifyNoMoreInteractions(bookmarkPort)
    }

    @Test
    fun `정상적으로 메시지를 북마크한다`() {
        val bookmarkPort = mock(BookmarkMessagePort::class.java)
        val loadMessagePort = mock(LoadMessagePort::class.java)
        val service = MessageBookmarkService(bookmarkPort, loadMessagePort)

        val messageId = "1"
        val userId = 1L
        val message = createMessage(messageId)
        val savedBookmark = MessageBookmark("b1", messageId, userId, Instant.now())

        `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(message)
        `when`(bookmarkPort.exists(messageId, userId)).thenReturn(false)
        `when`(bookmarkPort.saveBookmark(any())).thenReturn(savedBookmark)

        val result = service.bookmarkMessage(messageId, userId)

        assertThat(result).isEqualTo(savedBookmark)
        verify(bookmarkPort).saveBookmark(any())
    }

    private fun createMessage(id: String): ChatMessage {
        return ChatMessage(
            id = id,
            roomId = 1L,
            senderId = 2L,
            content = MessageContent("hi", MessageType.TEXT),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )
    }
}
