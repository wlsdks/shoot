package com.stark.shoot.domain.chat.message

import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("예약 메시지 테스트")
class ScheduledMessageTest {

    @Nested
    @DisplayName("예약 메시지 생성 시")
    inner class CreateScheduledMessage {

        @Test
        @DisplayName("필수 속성으로 예약 메시지를 생성할 수 있다")
        fun `필수 속성으로 예약 메시지를 생성할 수 있다`() {
            // given
            val roomId = 1L
            val senderId = 2L
            val content = MessageContent(
                text = "테스트 메시지",
                type = MessageType.TEXT
            )
            val scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)

            // when
            val scheduledMessage = ScheduledMessage(
                roomId = roomId,
                senderId = senderId,
                content = content,
                scheduledAt = scheduledAt
            )

            // then
            assertThat(scheduledMessage.id).isNull()
            assertThat(scheduledMessage.roomId).isEqualTo(roomId)
            assertThat(scheduledMessage.senderId).isEqualTo(senderId)
            assertThat(scheduledMessage.content).isEqualTo(content)
            assertThat(scheduledMessage.scheduledAt).isEqualTo(scheduledAt)
            assertThat(scheduledMessage.status).isEqualTo(ScheduledMessageStatus.PENDING)
            assertThat(scheduledMessage.metadata).isNotNull
            assertThat(scheduledMessage.createdAt).isNotNull
        }

        @Test
        @DisplayName("모든 속성으로 예약 메시지를 생성할 수 있다")
        fun `모든 속성으로 예약 메시지를 생성할 수 있다`() {
            // given
            val id = MessageId.from("message123")
            val roomId = 1L
            val senderId = 2L
            val content = MessageContent(
                text = "테스트 메시지",
                type = MessageType.TEXT
            )
            val scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)
            val createdAt = Instant.now().minus(1, ChronoUnit.HOURS)
            val status = ScheduledMessageStatus.SENT
            val metadata = ChatMessageMetadata(
                tempId = "temp123",
                needsUrlPreview = true
            )

            // when
            val scheduledMessage = ScheduledMessage(
                id = id,
                roomId = roomId,
                senderId = senderId,
                content = content,
                scheduledAt = scheduledAt,
                createdAt = createdAt,
                status = status,
                metadata = metadata
            )

            // then
            assertThat(scheduledMessage.id).isEqualTo(id)
            assertThat(scheduledMessage.roomId).isEqualTo(roomId)
            assertThat(scheduledMessage.senderId).isEqualTo(senderId)
            assertThat(scheduledMessage.content).isEqualTo(content)
            assertThat(scheduledMessage.scheduledAt).isEqualTo(scheduledAt)
            assertThat(scheduledMessage.createdAt).isEqualTo(createdAt)
            assertThat(scheduledMessage.status).isEqualTo(status)
            assertThat(scheduledMessage.metadata).isEqualTo(metadata)
        }
    }

    @Nested
    @DisplayName("예약 메시지 상태 확인 시")
    inner class CheckScheduledMessageStatus {

        @Test
        @DisplayName("기본 상태는 PENDING이다")
        fun `기본 상태는 PENDING이다`() {
            // given
            val scheduledMessage = ScheduledMessage(
                roomId = 1L,
                senderId = 2L,
                content = MessageContent(
                    text = "테스트 메시지",
                    type = MessageType.TEXT
                ),
                scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)
            )

            // then
            assertThat(scheduledMessage.status).isEqualTo(ScheduledMessageStatus.PENDING)
        }

        @Test
        @DisplayName("상태를 SENT로 설정할 수 있다")
        fun `상태를 SENT로 설정할 수 있다`() {
            // given
            val scheduledMessage = ScheduledMessage(
                roomId = 1L,
                senderId = 2L,
                content = MessageContent(
                    text = "테스트 메시지",
                    type = MessageType.TEXT
                ),
                scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS),
                status = ScheduledMessageStatus.SENT
            )

            // then
            assertThat(scheduledMessage.status).isEqualTo(ScheduledMessageStatus.SENT)
        }

        @Test
        @DisplayName("상태를 CANCELED로 설정할 수 있다")
        fun `상태를 CANCELED로 설정할 수 있다`() {
            // given
            val scheduledMessage = ScheduledMessage(
                roomId = 1L,
                senderId = 2L,
                content = MessageContent(
                    text = "테스트 메시지",
                    type = MessageType.TEXT
                ),
                scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS),
                status = ScheduledMessageStatus.CANCELED
            )

            // then
            assertThat(scheduledMessage.status).isEqualTo(ScheduledMessageStatus.CANCELED)
        }
    }

    @Nested
    @DisplayName("예약 메시지 내용 확인 시")
    inner class CheckScheduledMessageContent {

        @Test
        @DisplayName("텍스트 메시지를 예약할 수 있다")
        fun `텍스트 메시지를 예약할 수 있다`() {
            // given
            val text = "테스트 메시지"
            val content = MessageContent(
                text = text,
                type = MessageType.TEXT
            )

            // when
            val scheduledMessage = ScheduledMessage(
                roomId = 1L,
                senderId = 2L,
                content = content,
                scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)
            )

            // then
            assertThat(scheduledMessage.content.text).isEqualTo(text)
            assertThat(scheduledMessage.content.type).isEqualTo(MessageType.TEXT)
        }

        @Test
        @DisplayName("파일 메시지를 예약할 수 있다")
        fun `파일 메시지를 예약할 수 있다`() {
            // given
            val text = "파일 설명"
            val content = MessageContent(
                text = text,
                type = MessageType.FILE,
                attachments = listOf(
                    MessageContent.Attachment(
                        id = "attachment123",
                        url = "https://example.com/file.pdf",
                        filename = "file.pdf",
                        size = 1024L,
                        contentType = "application/pdf"
                    )
                )
            )

            // when
            val scheduledMessage = ScheduledMessage(
                roomId = 1L,
                senderId = 2L,
                content = content,
                scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)
            )

            // then
            assertThat(scheduledMessage.content.text).isEqualTo(text)
            assertThat(scheduledMessage.content.type).isEqualTo(MessageType.FILE)
            assertThat(scheduledMessage.content.attachments).hasSize(1)
            assertThat(scheduledMessage.content.attachments[0].filename).isEqualTo("file.pdf")
        }

        @Test
        @DisplayName("URL 메시지를 예약할 수 있다")
        fun `URL 메시지를 예약할 수 있다`() {
            // given
            val text = "https://example.com"
            val urlPreview = ChatMessageMetadata.UrlPreview(
                url = text,
                title = "Example Website",
                description = "This is an example website",
                imageUrl = "https://example.com/image.jpg",
                siteName = "Example"
            )
            val metadata = ChatMessageMetadata(
                needsUrlPreview = true,
                previewUrl = text,
                urlPreview = urlPreview
            )
            val content = MessageContent(
                text = text,
                type = MessageType.URL
            )

            // when
            val scheduledMessage = ScheduledMessage(
                roomId = 1L,
                senderId = 2L,
                content = content,
                scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS),
                metadata = metadata
            )

            // then
            assertThat(scheduledMessage.content.text).isEqualTo(text)
            assertThat(scheduledMessage.content.type).isEqualTo(MessageType.URL)
            assertThat(scheduledMessage.metadata.needsUrlPreview).isTrue()
            assertThat(scheduledMessage.metadata.previewUrl).isEqualTo(text)
            assertThat(scheduledMessage.metadata.urlPreview).isEqualTo(urlPreview)
        }

        @Test
        @DisplayName("이모티콘 메시지를 예약할 수 있다")
        fun `이모티콘 메시지를 예약할 수 있다`() {
            // given
            val text = "😊"
            val content = MessageContent(
                text = text,
                type = MessageType.EMOTICON
            )

            // when
            val scheduledMessage = ScheduledMessage(
                roomId = 1L,
                senderId = 2L,
                content = content,
                scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)
            )

            // then
            assertThat(scheduledMessage.content.text).isEqualTo(text)
            assertThat(scheduledMessage.content.type).isEqualTo(MessageType.EMOTICON)
        }
    }
}
