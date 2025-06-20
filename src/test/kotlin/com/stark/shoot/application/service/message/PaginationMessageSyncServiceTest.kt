package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.web.socket.dto.MessageSyncInfoDto
import com.stark.shoot.adapter.`in`.web.socket.dto.ReactionDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncResponseDto
import com.stark.shoot.adapter.`in`.web.socket.mapper.MessageSyncMapper
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.domain.chat.message.SyncDirection
import com.stark.shoot.domain.common.vo.MessageId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

@DisplayName("페이지네이션 메시지 동기화 서비스 테스트")
class PaginationMessageSyncServiceTest {

    private val loadMessagePort = mock(LoadMessagePort::class.java)
    private val messagingTemplate = mock(SimpMessagingTemplate::class.java)
    private val messageSyncMapper = mock(MessageSyncMapper::class.java)
    
    private val paginationMessageSyncService = PaginationMessageSyncService(
        loadMessagePort,
        messagingTemplate,
        messageSyncMapper
    )

    @Nested
    @DisplayName("메시지 동기화 Flow 조회 시")
    inner class GetChatMessagesFlow {
        
        @Test
        @DisplayName("초기 로드 시 메시지를 조회할 수 있다")
        fun `초기 로드 시 메시지를 조회할 수 있다`() = runBlocking {
            // given
            val roomId = 1L
            val userId = 2L
            
            val request = SyncRequestDto(
                roomId = roomId,
                userId = userId,
                lastMessageId = null,
                direction = SyncDirection.INITIAL
            )
            
            val messages = listOf(
                createChatMessage("1", roomId),
                createChatMessage("2", roomId),
                createChatMessage("3", roomId)
            )
            
            val messageFlow: Flow<ChatMessage> = flowOf(*messages.toTypedArray())
            
            val syncInfoDtos = messages.map { 
                MessageSyncInfoDto(
                    id = it.id ?: "",
                    timestamp = it.createdAt ?: Instant.now(),
                    senderId = it.senderId,
                    status = it.status.name,
                    content = MessageContentRequest(
                        text = it.content.text,
                        type = it.content.type
                    )
                )
            }
            
            `when`(loadMessagePort.findByRoomIdFlow(roomId, 50)).thenReturn(messageFlow)
            
            // Mock the mapper for each message
            for (i in messages.indices) {
                `when`(messageSyncMapper.toSyncInfoDto(messages[i])).thenReturn(syncInfoDtos[i])
            }
            
            // when
            val result = paginationMessageSyncService.getChatMessagesFlow(request).toList()
            
            // then
            assertThat(result).hasSize(3)
            assertThat(result).containsExactlyElementsOf(syncInfoDtos)
            
            verify(loadMessagePort).findByRoomIdFlow(roomId, 50)
            for (message in messages) {
                verify(messageSyncMapper).toSyncInfoDto(message)
            }
        }
        
        @Test
        @DisplayName("이전 메시지를 조회할 수 있다")
        fun `이전 메시지를 조회할 수 있다`() = runBlocking {
            // given
            val roomId = 1L
            val userId = 2L
            val lastMessageId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val lastMessageObjectId = ObjectId(lastMessageId)
            
            val request = SyncRequestDto(
                roomId = roomId,
                userId = userId,
                lastMessageId = lastMessageId,
                direction = SyncDirection.BEFORE
            )
            
            val messages = listOf(
                createChatMessage("1", roomId),
                createChatMessage("2", roomId)
            )
            
            val messageFlow: Flow<ChatMessage> = flowOf(*messages.toTypedArray())
            
            val syncInfoDtos = messages.map { 
                MessageSyncInfoDto(
                    id = it.id ?: "",
                    timestamp = it.createdAt ?: Instant.now(),
                    senderId = it.senderId,
                    status = it.status.name,
                    content = MessageContentRequest(
                        text = it.content.text,
                        type = it.content.type
                    )
                )
            }
            
            `when`(loadMessagePort.findByRoomIdAndBeforeIdFlow(roomId, lastMessageObjectId, 30)).thenReturn(messageFlow)
            
            // Mock the mapper for each message
            for (i in messages.indices) {
                `when`(messageSyncMapper.toSyncInfoDto(messages[i])).thenReturn(syncInfoDtos[i])
            }
            
            // when
            val result = paginationMessageSyncService.getChatMessagesFlow(request).toList()
            
            // then
            assertThat(result).hasSize(2)
            assertThat(result).containsExactlyElementsOf(syncInfoDtos)
            
            verify(loadMessagePort).findByRoomIdAndBeforeIdFlow(roomId, lastMessageObjectId, 30)
            for (message in messages) {
                verify(messageSyncMapper).toSyncInfoDto(message)
            }
        }
    }
    
    @Nested
    @DisplayName("메시지 전송 시")
    inner class SendMessagesToUser {
        
        @Test
        @DisplayName("사용자에게 메시지를 전송할 수 있다")
        fun `사용자에게 메시지를 전송할 수 있다`() {
            // given
            val roomId = 1L
            val userId = 2L
            
            val request = SyncRequestDto(
                roomId = roomId,
                userId = userId,
                lastMessageId = null,
                direction = SyncDirection.INITIAL
            )
            
            val messages = listOf(
                MessageSyncInfoDto(
                    id = "1",
                    timestamp = Instant.now(),
                    senderId = 3L,
                    status = MessageStatus.SAVED.name,
                    content = MessageContentRequest(
                        text = "테스트 메시지 1",
                        type = MessageType.TEXT
                    )
                ),
                MessageSyncInfoDto(
                    id = "2",
                    timestamp = Instant.now(),
                    senderId = 4L,
                    status = MessageStatus.SAVED.name,
                    content = MessageContentRequest(
                        text = "테스트 메시지 2",
                        type = MessageType.TEXT
                    )
                )
            )
            
            // when
            paginationMessageSyncService.sendMessagesToUser(request, messages)
            
            // then
            verify(messagingTemplate).convertAndSendToUser(
                eq(userId.toString()),
                eq("/queue/sync"),
                any(SyncResponseDto::class.java)
            )
        }
    }
    
    private fun createChatMessage(id: String, roomId: Long): ChatMessage {
        return ChatMessage(
            id = MessageId.from(id),
            roomId = roomId,
            senderId = 3L,
            content = MessageContent("테스트 메시지 $id", MessageType.TEXT),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )
    }
}