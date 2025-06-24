package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

@DisplayName("채팅방 메타데이터 도메인 서비스 테스트")
class ChatRoomMetadataDomainServiceTest {
    private val service = ChatRoomMetadataDomainService()

    @Test
    fun `메시지 ID가 없으면 예외가 발생한다`() {
        val room =
            ChatRoom(
                title = ChatRoomTitle.from("room"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L))
            )
        val msg = ChatMessage(
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent("hi", MessageType.TEXT),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )
        assertThrows<IllegalArgumentException> { service.updateChatRoomWithNewMessage(room, msg) }
    }

    @Test
    fun `새 메시지로 채팅방 메타데이터를 업데이트할 수 있다`() {
        val room = ChatRoom(
            id = ChatRoomId.from(1L),
            title = ChatRoomTitle.from("room"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(UserId.from(1L))
        )
        val msg = ChatMessage(
            id = MessageId.from("m1"),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent("hi", MessageType.TEXT),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )
        val updated = service.updateChatRoomWithNewMessage(room, msg)
        assertThat(updated.lastMessageId?.value).isEqualTo("m1")
        assertThat(updated.lastActiveAt).isAfter(room.lastActiveAt)
    }

}
