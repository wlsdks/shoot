package com.stark.shoot.adapter.persistence.message.preview

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.preview.RedisUrlPreviewCacheAdapter
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.infrastructure.config.redis.RedisUtilService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.time.Instant

@DisplayName("Redis URL 프리뷰 캐시 어댑터 테스트")
class RedisUrlPreviewCacheAdapterTest {

    private val redisTemplate = mock(StringRedisTemplate::class.java)
    private val valueOps = mock(ValueOperations::class.java) as ValueOperations<String, String>
    private val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(KotlinModule.Builder().build())
    }
    private val redisUtilService = mock(RedisUtilService::class.java)

    private val adapter = RedisUrlPreviewCacheAdapter(redisTemplate, objectMapper, redisUtilService)

    @Test
    @DisplayName("[happy] 캐시된 URL 프리뷰를 조회할 수 있다")
    fun `캐시된 URL 프리뷰를 조회할 수 있다`() {
        val url = "https://example.com"
        val key = "url_preview:hash"
        val preview = ChatMessageMetadata.UrlPreview(url, "t", "d", "i", "s", Instant.now())
        `when`(redisUtilService.createHashKey("url_preview:", url)).thenReturn(key)
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        `when`(valueOps.get(key)).thenReturn(objectMapper.writeValueAsString(preview))

        val result = adapter.getCachedUrlPreview(url)

        assertThat(result).usingRecursiveComparison().ignoringFields("fetchedAt").isEqualTo(preview)
    }

    @Test
    @DisplayName("[happy] URL 프리뷰를 캐시에 저장할 수 있다")
    fun `URL 프리뷰를 캐시에 저장할 수 있다`() {
        val url = "https://example.com"
        val key = "url_preview:hash"
        val preview = ChatMessageMetadata.UrlPreview(url, "t", "d", "i", "s", Instant.now())
        `when`(redisUtilService.createHashKey("url_preview:", url)).thenReturn(key)
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        doNothing().`when`(valueOps).set(anyString(), anyString(), any(Duration::class.java))

        adapter.cacheUrlPreview(url, preview)

        verify(valueOps).set(eq(key), any(), eq(Duration.ofDays(7)))
    }
}
