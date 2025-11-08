package com.stark.shoot.adapter.`in`.event.notification

import com.stark.shoot.application.service.event.notification.ChatNotificationFactory
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("ChatNotificationFactory 테스트")
class ChatNotificationFactoryTest {

    private val factory = ChatNotificationFactory()

    private fun sampleMessage(text: String): ChatMessage {
        return ChatMessage(
            id = MessageId.from("m1"),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent(text, MessageType.TEXT),
            status = MessageStatus.SENT,
            createdAt = Instant.now()
        )
    }

    @Test
    @DisplayName("[happy] 멘션 알림을 생성한다")
    fun `멘션 알림을 생성한다`() {
        val msg = sampleMessage("hello world")
        val notification = factory.createMentionNotification(1L, msg)
        assertThat(notification.userId).isEqualTo(UserId.from(1L))
        assertThat(notification.type).isEqualTo(NotificationType.MENTION)
        assertThat(notification.metadata["senderId"]).isEqualTo("2")
        assertThat(notification.metadata["messageId"]).isEqualTo("m1")
    }

    @Test
    @DisplayName("[happy] 메시지 알림을 생성한다")
    fun `메시지 알림을 생성한다`() {
        val msg = sampleMessage("a".repeat(60))
        val notification = factory.createMessageNotification(3L, msg)
        assertThat(notification.type).isEqualTo(NotificationType.NEW_MESSAGE)
        assertThat(notification.message.value.length).isLessThanOrEqualTo(53)
    }

    @Test
    @DisplayName("[happy] 반응 알림을 생성한다")
    fun `반응 알림을 생성한다`() {
        val notification = factory.createReactionNotification(
            userId = UserId.from(1L),
            reactingUserId = UserId.from(2L),
            messageId = MessageId.from("m2"),
            roomId = ChatRoomId.from(1L),
            reactionType = "like"
        )
        assertThat(notification.type).isEqualTo(NotificationType.REACTION)
        assertThat(notification.metadata["reactionType"]).isEqualTo("like")
        assertThat(notification.metadata["messageId"]).isEqualTo(MessageId.from("m2"))
    }
}
