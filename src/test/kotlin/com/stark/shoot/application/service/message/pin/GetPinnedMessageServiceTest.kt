package com.stark.shoot.application.service.message.pin

import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("고정 메시지 조회 서비스 테스트")
class GetPinnedMessageServiceTest {

    private val loadMessagePort = mock(LoadMessagePort::class.java)
    
    private val getPinnedMessageService = GetPinnedMessageService(
        loadMessagePort
    )

    @Nested
    @DisplayName("고정 메시지 조회 시")
    inner class GetPinnedMessages {
        
        @Test
        @DisplayName("채팅방의 고정 메시지를 조회할 수 있다")
        fun `채팅방의 고정 메시지를 조회할 수 있다`() {
            // given
            val roomId = 1L
            
            val pinnedMessage = ChatMessage(
                id = "5f9f1b9b9c9d1b9b9c9d1b9b",
                roomId = roomId,
                senderId = 2L,
                content = MessageContent("고정된 메시지", MessageType.TEXT),
                status = MessageStatus.SAVED,
                createdAt = Instant.now(),
                isPinned = true,
                pinnedBy = 3L,
                pinnedAt = Instant.now()
            )
            
            `when`(loadMessagePort.findPinnedMessagesByRoomId(roomId, 1)).thenReturn(listOf(pinnedMessage))
            
            // when
            val result = getPinnedMessageService.getPinnedMessages(roomId)
            
            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(pinnedMessage)
            assertThat(result[0].isPinned).isTrue()
            
            verify(loadMessagePort).findPinnedMessagesByRoomId(roomId, 1)
        }
        
        @Test
        @DisplayName("고정 메시지가 없는 경우 빈 목록을 반환한다")
        fun `고정 메시지가 없는 경우 빈 목록을 반환한다`() {
            // given
            val roomId = 1L
            
            `when`(loadMessagePort.findPinnedMessagesByRoomId(roomId, 1)).thenReturn(emptyList())
            
            // when
            val result = getPinnedMessageService.getPinnedMessages(roomId)
            
            // then
            assertThat(result).isEmpty()
            
            verify(loadMessagePort).findPinnedMessagesByRoomId(roomId, 1)
        }
    }
}