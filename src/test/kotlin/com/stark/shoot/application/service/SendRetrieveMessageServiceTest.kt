package com.stark.shoot.application.service

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.service.message.SendMessageService
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomMetadata
import org.bson.types.ObjectId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*

class SendRetrieveMessageServiceTest {

    private val saveMessagePort = mock(SaveMessagePort::class.java)
    private val loadChatRoomPort = mock(LoadChatRoomPort::class.java)
    private val eventPublisher = mock(EventPublisher::class.java)

    // 일단 위에서 생성한 mock 객체를 사용하여 SendMessageService 객체를 생성한다.
    private val sendMessageService = SendMessageService(saveMessagePort, loadChatRoomPort, eventPublisher)


    @DisplayName("sendMessage 성공 테스트")
    @Test
    fun sendMessageSuccessTest() {
        val roomId = ObjectId().toString()
        val senderId = ObjectId().toString()
        val chatRoom = ChatRoom(
            id = roomId,
            participants = setOf(senderId),
            metadata = mock(ChatRoomMetadata::class.java)
        )
        val messageContent = MessageContent(
            text = "Hello, World!",
            type = MessageType.TEXT
        )
        val message = ChatMessage(
            roomId = roomId,
            senderId = senderId,
            content = messageContent,
            status = MessageStatus.SENT
        )

        // Mock 설정
        `when`(loadChatRoomPort.findById(ObjectId(roomId))).thenReturn(chatRoom)
        `when`(saveMessagePort.save(any(ChatMessage::class.java))).thenReturn(message)

        // 메서드 호출
        val result = sendMessageService.sendMessage(roomId, senderId, message)

        // 결과 검증
        verify(loadChatRoomPort).findById(ObjectId(roomId))
        verify(saveMessagePort).save(any(ChatMessage::class.java))
        verify(eventPublisher).publish(any())
        assert(result.content.text == "Hello, World!")
    }

    @DisplayName("sendMessage 실패 테스트 - 채팅방 없음")
    @Test
    fun sendMessageFailTest() {
        val roomId = ObjectId().toString()
        val senderId = ObjectId().toString()
        val messageContent = MessageContent(
            text = "Hello",
            type = MessageType.TEXT
        )

        // Mock 설정
        `when`(loadChatRoomPort.findById(ObjectId(roomId))).thenReturn(null)

        // 예외 발생 검증
        assertThrows<ResourceNotFoundException> {
            sendMessageService.sendMessage(
                roomId, senderId, ChatMessage(
                    roomId = roomId,
                    senderId = senderId,
                    content = messageContent,
                    status = MessageStatus.SENT
                )
            )
        }

        // 호출 검증
        verify(loadChatRoomPort).findById(ObjectId(roomId))
        verifyNoInteractions(saveMessagePort, eventPublisher)
    }

}