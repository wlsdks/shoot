package com.stark.shoot.domain.chat.message

import com.stark.shoot.domain.chat.message.type.MessageType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("메시지 내용 테스트")
class MessageContentTest {

    @Nested
    @DisplayName("메시지 내용 생성 시")
    inner class CreateMessageContent {

        @Test
        @DisplayName("기본 텍스트 메시지 내용을 생성할 수 있다")
        fun `기본 텍스트 메시지 내용을 생성할 수 있다`() {
            // given
            val text = "안녕하세요"
            val type = MessageType.TEXT

            // when
            val content = MessageContent(
                text = text,
                type = type
            )

            // then
            assertThat(content.text).isEqualTo(text)
            assertThat(content.type).isEqualTo(type)
            assertThat(content.isEdited).isFalse()
            assertThat(content.isDeleted).isFalse()
            assertThat(content.attachments).isEmpty()
        }

        @Test
        @DisplayName("파일 타입 메시지 내용을 생성할 수 있다")
        fun `파일 타입 메시지 내용을 생성할 수 있다`() {
            // given
            val text = "파일 설명"
            val type = MessageType.FILE
            val attachment = Attachment(
                id = "file-123",
                url = "https://example.com/file.pdf",
                filename = "example.pdf",
                size = 1024,
                contentType = "application/pdf"
            )

            // when
            val content = MessageContent(
                text = text,
                type = type,
                attachments = listOf(attachment)
            )

            // then
            assertThat(content.text).isEqualTo(text)
            assertThat(content.type).isEqualTo(type)
            assertThat(content.isEdited).isFalse()
            assertThat(content.isDeleted).isFalse()
            assertThat(content.attachments).hasSize(1)
            assertThat(content.attachments[0].id).isEqualTo(attachment.id)
            assertThat(content.attachments[0].url).isEqualTo(attachment.url)
            assertThat(content.attachments[0].filename).isEqualTo(attachment.filename)
            assertThat(content.attachments[0].size).isEqualTo(attachment.size)
            assertThat(content.attachments[0].contentType).isEqualTo(attachment.contentType)
        }

        @Test
        @DisplayName("수정된 메시지 내용을 생성할 수 있다")
        fun `수정된 메시지 내용을 생성할 수 있다`() {
            // given
            val text = "수정된 내용"
            val type = MessageType.TEXT
            val isEdited = true

            // when
            val content = MessageContent(
                text = text,
                type = type,
                isEdited = isEdited
            )

            // then
            assertThat(content.text).isEqualTo(text)
            assertThat(content.type).isEqualTo(type)
            assertThat(content.isEdited).isTrue()
            assertThat(content.isDeleted).isFalse()
        }

        @Test
        @DisplayName("삭제된 메시지 내용을 생성할 수 있다")
        fun `삭제된 메시지 내용을 생성할 수 있다`() {
            // given
            val text = "삭제된 메시지입니다"
            val type = MessageType.TEXT
            val isDeleted = true

            // when
            val content = MessageContent(
                text = text,
                type = type,
                isDeleted = isDeleted
            )

            // then
            assertThat(content.text).isEqualTo(text)
            assertThat(content.type).isEqualTo(type)
            assertThat(content.isEdited).isFalse()
            assertThat(content.isDeleted).isTrue()
        }

        @Test
        @DisplayName("메타데이터를 포함한 메시지 내용을 생성할 수 있다")
        fun `메타데이터를 포함한 메시지 내용을 생성할 수 있다`() {
            // given
            val text = "메타데이터 포함 메시지"
            val type = MessageType.TEXT
            val metadata = ChatMessageMetadata(
                tempId = "temp-123"
            )

            // when
            val content = MessageContent(
                text = text,
                type = type,
                metadata = metadata
            )

            // then
            assertThat(content.text).isEqualTo(text)
            assertThat(content.type).isEqualTo(type)
            assertThat(content.metadata).isEqualTo(metadata)
            assertThat(content.metadata?.tempId).isEqualTo("temp-123")
        }
    }

    @Nested
    @DisplayName("메시지 내용 복사 시")
    inner class CopyMessageContent {

        @Test
        @DisplayName("메시지 내용을 복사하여 수정할 수 있다")
        fun `메시지 내용을 복사하여 수정할 수 있다`() {
            // given
            val originalContent = MessageContent(
                text = "원본 메시지",
                type = MessageType.TEXT
            )
            val newText = "수정된 메시지"

            // when
            val copiedContent = originalContent.copy(
                text = newText,
                isEdited = true
            )

            // then
            assertThat(copiedContent.text).isEqualTo(newText)
            assertThat(copiedContent.type).isEqualTo(originalContent.type)
            assertThat(copiedContent.isEdited).isTrue()
            assertThat(copiedContent.isDeleted).isEqualTo(originalContent.isDeleted)
        }

        @Test
        @DisplayName("메시지 내용을 복사하여 삭제 상태로 변경할 수 있다")
        fun `메시지 내용을 복사하여 삭제 상태로 변경할 수 있다`() {
            // given
            val originalContent = MessageContent(
                text = "원본 메시지",
                type = MessageType.TEXT
            )

            // when
            val deletedContent = originalContent.copy(
                isDeleted = true
            )

            // then
            assertThat(deletedContent.text).isEqualTo(originalContent.text)
            assertThat(deletedContent.type).isEqualTo(originalContent.type)
            assertThat(deletedContent.isEdited).isEqualTo(originalContent.isEdited)
            assertThat(deletedContent.isDeleted).isTrue()
        }
    }
}
