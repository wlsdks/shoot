package com.stark.shoot.domain.chat.message

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("URL 미리보기 테스트")
class UrlPreviewTest {

    @Nested
    @DisplayName("URL 미리보기 생성 시")
    inner class CreateUrlPreview {
    
        @Test
        @DisplayName("필수 속성으로 URL 미리보기를 생성할 수 있다")
        fun `필수 속성으로 URL 미리보기를 생성할 수 있다`() {
            // given
            val url = "https://example.com"
            
            // when
            val urlPreview = UrlPreview(
                url = url,
                title = null,
                description = null,
                imageUrl = null
            )
            
            // then
            assertThat(urlPreview.url).isEqualTo(url)
            assertThat(urlPreview.title).isNull()
            assertThat(urlPreview.description).isNull()
            assertThat(urlPreview.imageUrl).isNull()
            assertThat(urlPreview.siteName).isNull()
            assertThat(urlPreview.fetchedAt).isNotNull()
        }
        
        @Test
        @DisplayName("모든 속성으로 URL 미리보기를 생성할 수 있다")
        fun `모든 속성으로 URL 미리보기를 생성할 수 있다`() {
            // given
            val url = "https://example.com"
            val title = "Example Website"
            val description = "This is an example website"
            val imageUrl = "https://example.com/image.jpg"
            val siteName = "Example"
            val fetchedAt = Instant.now().minus(1, ChronoUnit.HOURS)
            
            // when
            val urlPreview = UrlPreview(
                url = url,
                title = title,
                description = description,
                imageUrl = imageUrl,
                siteName = siteName,
                fetchedAt = fetchedAt
            )
            
            // then
            assertThat(urlPreview.url).isEqualTo(url)
            assertThat(urlPreview.title).isEqualTo(title)
            assertThat(urlPreview.description).isEqualTo(description)
            assertThat(urlPreview.imageUrl).isEqualTo(imageUrl)
            assertThat(urlPreview.siteName).isEqualTo(siteName)
            assertThat(urlPreview.fetchedAt).isEqualTo(fetchedAt)
        }
    }
    
    @Nested
    @DisplayName("URL 미리보기 정보 확인 시")
    inner class CheckUrlPreviewInfo {
    
        @Test
        @DisplayName("URL 정보를 확인할 수 있다")
        fun `URL 정보를 확인할 수 있다`() {
            // given
            val url = "https://example.com"
            val urlPreview = UrlPreview(
                url = url,
                title = "Example Website",
                description = "This is an example website",
                imageUrl = "https://example.com/image.jpg"
            )
            
            // then
            assertThat(urlPreview.url).isEqualTo(url)
        }
        
        @Test
        @DisplayName("제목 정보를 확인할 수 있다")
        fun `제목 정보를 확인할 수 있다`() {
            // given
            val title = "Example Website"
            val urlPreview = UrlPreview(
                url = "https://example.com",
                title = title,
                description = "This is an example website",
                imageUrl = "https://example.com/image.jpg"
            )
            
            // then
            assertThat(urlPreview.title).isEqualTo(title)
        }
        
        @Test
        @DisplayName("설명 정보를 확인할 수 있다")
        fun `설명 정보를 확인할 수 있다`() {
            // given
            val description = "This is an example website"
            val urlPreview = UrlPreview(
                url = "https://example.com",
                title = "Example Website",
                description = description,
                imageUrl = "https://example.com/image.jpg"
            )
            
            // then
            assertThat(urlPreview.description).isEqualTo(description)
        }
        
        @Test
        @DisplayName("이미지 URL 정보를 확인할 수 있다")
        fun `이미지 URL 정보를 확인할 수 있다`() {
            // given
            val imageUrl = "https://example.com/image.jpg"
            val urlPreview = UrlPreview(
                url = "https://example.com",
                title = "Example Website",
                description = "This is an example website",
                imageUrl = imageUrl
            )
            
            // then
            assertThat(urlPreview.imageUrl).isEqualTo(imageUrl)
        }
        
        @Test
        @DisplayName("사이트 이름 정보를 확인할 수 있다")
        fun `사이트 이름 정보를 확인할 수 있다`() {
            // given
            val siteName = "Example"
            val urlPreview = UrlPreview(
                url = "https://example.com",
                title = "Example Website",
                description = "This is an example website",
                imageUrl = "https://example.com/image.jpg",
                siteName = siteName
            )
            
            // then
            assertThat(urlPreview.siteName).isEqualTo(siteName)
        }
    }
    
    @Nested
    @DisplayName("URL 미리보기 생성 시간 확인 시")
    inner class CheckUrlPreviewFetchedAt {
    
        @Test
        @DisplayName("생성 시간이 자동으로 설정된다")
        fun `생성 시간이 자동으로 설정된다`() {
            // given
            val beforeCreation = Instant.now().minusMillis(100)
            
            // when
            val urlPreview = UrlPreview(
                url = "https://example.com",
                title = "Example Website",
                description = "This is an example website",
                imageUrl = "https://example.com/image.jpg"
            )
            val afterCreation = Instant.now().plusMillis(100)
            
            // then
            assertThat(urlPreview.fetchedAt).isNotNull()
            assertThat(urlPreview.fetchedAt).isAfterOrEqualTo(beforeCreation)
            assertThat(urlPreview.fetchedAt).isBeforeOrEqualTo(afterCreation)
        }
        
        @Test
        @DisplayName("생성 시간을 명시적으로 설정할 수 있다")
        fun `생성 시간을 명시적으로 설정할 수 있다`() {
            // given
            val fetchedAt = Instant.now().minus(1, ChronoUnit.HOURS)
            
            // when
            val urlPreview = UrlPreview(
                url = "https://example.com",
                title = "Example Website",
                description = "This is an example website",
                imageUrl = "https://example.com/image.jpg",
                fetchedAt = fetchedAt
            )
            
            // then
            assertThat(urlPreview.fetchedAt).isEqualTo(fetchedAt)
        }
    }
}