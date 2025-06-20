package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.preview

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.config.redis.RedisUtilService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

@Adapter
class RedisUrlPreviewCacheAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val redisUtilService: RedisUtilService
) : CacheUrlPreviewPort {

    private val logger = KotlinLogging.logger {}
    private val CACHE_PREFIX = "url_preview:"
    private val CACHE_TTL = Duration.ofDays(7) // 7일간 캐싱

    /**
     * URL 미리보기를 Redis에서 조회합니다.
     *
     * @param url 조회할 URL
     * @return URL 미리보기 정보 (캐시된 경우)
     */
    override fun getCachedUrlPreview(
        url: String
    ): ChatMessageMetadata.UrlPreview? {
        try {
            val key = generateKey(url)
            val cachedValue = redisTemplate.opsForValue().get(key)

            if (cachedValue != null) {
                return objectMapper.readValue(cachedValue, ChatMessageMetadata.UrlPreview::class.java)
            }
            return null
        } catch (e: Exception) {
            logger.error(e) { "Redis에서 URL 미리보기 조회 중 오류: $url" }
            return null
        }
    }

    /**
     * URL 미리보기를 Redis에 저장합니다.
     *
     * @param url 저장할 URL
     * @param preview URL 미리보기 정보
     */
    override fun cacheUrlPreview(
        url: String,
        preview: ChatMessageMetadata.UrlPreview
    ) {
        try {
            val key = generateKey(url)
            val value = objectMapper.writeValueAsString(preview)

            redisTemplate.opsForValue().set(key, value, CACHE_TTL)
        } catch (e: Exception) {
            logger.error(e) { "Redis에 URL 미리보기 저장 중 오류: $url" }
        }
    }

    /**
     * URL에 대한 캐시 키를 생성합니다.
     * RedisUtilService의 createHashKey 메서드를 사용하여 해시 기반 키를 생성합니다.
     * 기본적으로 같은 문자열에 대해 Kotlin(Java)의 hashCode() 메서드는 항상 동일한 해시 값을 반환합니다.
     * 이론상 해시 충돌이 있을수도 있지만 거의 없다고 보는게 좋습니다.
     *
     * @param url URL
     * @return 캐시 키
     */
    private fun generateKey(
        url: String
    ): String {
        return redisUtilService.createHashKey(CACHE_PREFIX, url)
    }

}
