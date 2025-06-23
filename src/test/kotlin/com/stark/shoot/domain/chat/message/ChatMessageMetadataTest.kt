package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.`in`.web.dto.message.toRequestDto
import com.stark.shoot.adapter.`in`.web.dto.message.toResponseDto
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("메시지 메타데이터 테스트")
class ChatMessageMetadataTest {

    @Nested
    @DisplayName("메시지 메타데이터 생성 시")
    inner class CreateChatMessageMetadata {

        @Test
        @DisplayName("기본 메타데이터를 생성할 수 있다")
        fun `기본 메타데이터를 생성할 수 있다`() {
            // when
            val metadata = ChatMessageMetadata()

            // then
            assertThat(metadata.tempId).isNull()
            assertThat(metadata.needsUrlPreview).isFalse()
            assertThat(metadata.previewUrl).isNull()
            assertThat(metadata.urlPreview).isNull()
            assertThat(metadata.readAt).isNull()
        }

        @Test
        @DisplayName("임시 ID를 포함한 메타데이터를 생성할 수 있다")
        fun `임시 ID를 포함한 메타데이터를 생성할 수 있다`() {
            // given
            val tempId = "temp-123"

            // when
            val metadata = ChatMessageMetadata(tempId = tempId)

            // then
            assertThat(metadata.tempId).isEqualTo(tempId)
            assertThat(metadata.needsUrlPreview).isFalse()
            assertThat(metadata.previewUrl).isNull()
            assertThat(metadata.urlPreview).isNull()
            assertThat(metadata.readAt).isNull()
        }

        @Test
        @DisplayName("URL 미리보기 정보를 포함한 메타데이터를 생성할 수 있다")
        fun `URL 미리보기 정보를 포함한 메타데이터를 생성할 수 있다`() {
            // given
            val needsUrlPreview = true
            val previewUrl = "https://example.com"

            // when
            val metadata = ChatMessageMetadata(
                needsUrlPreview = needsUrlPreview,
                previewUrl = previewUrl
            )

            // then
            assertThat(metadata.needsUrlPreview).isTrue()
            assertThat(metadata.previewUrl).isEqualTo(previewUrl)
            assertThat(metadata.urlPreview).isNull()
        }

        @Test
        @DisplayName("URL 미리보기 결과를 포함한 메타데이터를 생성할 수 있다")
        fun `URL 미리보기 결과를 포함한 메타데이터를 생성할 수 있다`() {
            // given
            val urlPreview = ChatMessageMetadata.UrlPreview(
                url = "https://example.com",
                title = "Example Domain",
                description = "This domain is for use in examples",
                imageUrl = "https://example.com/image.jpg",
                siteName = "Example"
            )

            // when
            val metadata = ChatMessageMetadata(
                urlPreview = urlPreview
            )

            // then
            assertThat(metadata.urlPreview).isEqualTo(urlPreview)
            assertThat(metadata.urlPreview?.url).isEqualTo(urlPreview.url)
            assertThat(metadata.urlPreview?.title).isEqualTo(urlPreview.title)
            assertThat(metadata.urlPreview?.description).isEqualTo(urlPreview.description)
            assertThat(metadata.urlPreview?.imageUrl).isEqualTo(urlPreview.imageUrl)
            assertThat(metadata.urlPreview?.siteName).isEqualTo(urlPreview.siteName)
        }

        @Test
        @DisplayName("읽은 시간을 포함한 메타데이터를 생성할 수 있다")
        fun `읽은 시간을 포함한 메타데이터를 생성할 수 있다`() {
            // given
            val readAt = Instant.now()

            // when
            val metadata = ChatMessageMetadata(
                readAt = readAt
            )

            // then
            assertThat(metadata.readAt).isEqualTo(readAt)
        }
    }

    @Nested
    @DisplayName("메시지 메타데이터 복사 시")
    inner class CopyChatMessageMetadata {

        @Test
        @DisplayName("메타데이터를 복사하여 URL 미리보기 정보를 업데이트할 수 있다")
        fun `메타데이터를 복사하여 URL 미리보기 정보를 업데이트할 수 있다`() {
            // given
            val originalMetadata = ChatMessageMetadata(
                tempId = "temp-123",
                needsUrlPreview = true,
                previewUrl = "https://example.com"
            )

            val urlPreview = ChatMessageMetadata.UrlPreview(
                url = "https://example.com",
                title = "Example Domain",
                description = "This domain is for use in examples",
                imageUrl = "https://example.com/image.jpg",
                siteName = "Example"
            )

            // when
            val updatedMetadata = originalMetadata.copy(
                needsUrlPreview = false,
                urlPreview = urlPreview
            )

            // then
            assertThat(updatedMetadata.tempId).isEqualTo(originalMetadata.tempId)
            assertThat(updatedMetadata.needsUrlPreview).isFalse()
            assertThat(updatedMetadata.previewUrl).isEqualTo(originalMetadata.previewUrl)
            assertThat(updatedMetadata.urlPreview).isEqualTo(urlPreview)
        }

        @Test
        @DisplayName("메타데이터를 복사하여 읽은 시간을 업데이트할 수 있다")
        fun `메타데이터를 복사하여 읽은 시간을 업데이트할 수 있다`() {
            // given
            val originalMetadata = ChatMessageMetadata(
                tempId = "temp-123"
            )

            val readAt = Instant.now()

            // when
            val updatedMetadata = originalMetadata.copy(
                readAt = readAt
            )

            // then
            assertThat(updatedMetadata.tempId).isEqualTo(originalMetadata.tempId)
            assertThat(updatedMetadata.readAt).isEqualTo(readAt)
        }
    }

    @Nested
    @DisplayName("메시지 메타데이터 변환 시")
    inner class ConvertChatMessageMetadata {

        @Test
        @DisplayName("메타데이터를 응답 DTO로 변환할 수 있다")
        fun `메타데이터를 응답 DTO로 변환할 수 있다`() {
            // given
            val tempId = "temp-123"
            val needsUrlPreview = true
            val previewUrl = "https://example.com"
            val urlPreview = ChatMessageMetadata.UrlPreview(
                url = "https://example.com",
                title = "Example Domain",
                description = "This domain is for use in examples",
                imageUrl = "https://example.com/image.jpg",
                siteName = "Example"
            )

            val metadata = ChatMessageMetadata(
                tempId = tempId,
                needsUrlPreview = needsUrlPreview,
                previewUrl = previewUrl,
                urlPreview = urlPreview
            )

            // when
            val responseDto = metadata.toResponseDto()

            // then
            assertThat(responseDto.tempId).isEqualTo(tempId)
            assertThat(responseDto.needsUrlPreview).isEqualTo(needsUrlPreview)
            assertThat(responseDto.previewUrl).isEqualTo(previewUrl)
            assertThat(responseDto.urlPreview).isEqualTo(urlPreview)
        }

        @Test
        @DisplayName("메타데이터를 요청 DTO로 변환할 수 있다")
        fun `메타데이터를 요청 DTO로 변환할 수 있다`() {
            // given
            val tempId = "temp-123"
            val needsUrlPreview = true
            val previewUrl = "https://example.com"
            val urlPreview = ChatMessageMetadata.UrlPreview(
                url = "https://example.com",
                title = "Example Domain",
                description = "This domain is for use in examples",
                imageUrl = "https://example.com/image.jpg",
                siteName = "Example"
            )

            val metadata = ChatMessageMetadata(
                tempId = tempId,
                needsUrlPreview = needsUrlPreview,
                previewUrl = previewUrl,
                urlPreview = urlPreview
            )

            // when
            val requestDto = metadata.toRequestDto()

            // then
            assertThat(requestDto.tempId).isEqualTo(tempId)
            assertThat(requestDto.needsUrlPreview).isEqualTo(needsUrlPreview)
            assertThat(requestDto.previewUrl).isEqualTo(previewUrl)
            assertThat(requestDto.urlPreview?.url).isEqualTo(urlPreview.url)
            assertThat(requestDto.urlPreview?.title).isEqualTo(urlPreview.title)
            assertThat(requestDto.urlPreview?.description).isEqualTo(urlPreview.description)
            assertThat(requestDto.urlPreview?.imageUrl).isEqualTo(urlPreview.imageUrl)
            assertThat(requestDto.urlPreview?.siteName).isEqualTo(urlPreview.siteName)
        }
    }
}