package com.stark.shoot.domain.service.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageForwardDomainService
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("메시지 전달 도메인 서비스 테스트")
class MessageForwardDomainServiceTest {
    private val service = MessageForwardDomainService()

    @Test
    fun `메시지 전달용 내용을 생성할 수 있다`() {
        val original = ChatMessage(
            id = MessageId.from("m1"),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent("hello", MessageType.TEXT),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )

        val content = service.createForwardedContent(original)
        assertThat(content.text).isEqualTo("[Forwarded] hello")
    }

    @Test
    fun `전달 메시지를 생성할 수 있다`() {
        val content = MessageContent("[Forwarded] hi", MessageType.TEXT)
        val msg = service.createForwardedMessage(
            ChatRoomId.from(3L),
            UserId.from(4L),
            content
        )

        assertThat(msg.roomId).isEqualTo(3L)
        assertThat(msg.senderId).isEqualTo(4L)
        assertThat(msg.content).isEqualTo(content)
        assertThat(msg.status).isEqualTo(MessageStatus.SAVED)
    }

}
