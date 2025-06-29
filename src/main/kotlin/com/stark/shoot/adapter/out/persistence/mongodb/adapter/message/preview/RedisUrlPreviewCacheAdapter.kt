package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.preview

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.config.redis.RedisUtilService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Adapter
class RedisUrlPreviewCacheAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val redisUtilService: RedisUtilService
) : CacheUrlPreviewPort {

    private val logger = KotlinLogging.logger {}

    // 메모리 내 캐시 (최근 조회한 URL 미리보기를 캐싱하여 Redis 조회 최소화)
    private val localCache = ConcurrentHashMap<String, CacheEntry>()

    companion object {
        // 상수를 private으로 선언하여 캡슐화
        private const val CACHE_PREFIX = "url_preview:"
        private val CACHE_TTL = Duration.ofDays(7) // 7일간 캐싱
        private val LOCAL_CACHE_TTL_MS = Duration.ofMinutes(10).toMillis() // 로컬 캐시 10분 유지
        private const val LOCAL_CACHE_MAX_SIZE = 500 // 로컬 캐시 최대 크기

        // 배치 작업을 위한 최대 크기
        private const val BATCH_MAX_SIZE = 20
    }

    // 캐시 항목 클래스 (로컬 캐시용)
    private data class CacheEntry(
        val preview: ChatMessageMetadata.UrlPreview,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > LOCAL_CACHE_TTL_MS
    }

    /**
     * URL 미리보기를 조회합니다. 로컬 캐시 → Redis 순으로 조회합니다.
     *
     * @param url 조회할 URL
     * @return URL 미리보기 정보 (캐시된 경우)
     */
    override fun getCachedUrlPreview(url: String): ChatMessageMetadata.UrlPreview? {
        if (url.isBlank()) {
            return null
        }

        try {
            // 1. 로컬 캐시에서 먼저 조회
            val localEntry = localCache[url]
            if (localEntry != null) {
                // 만료된 항목은 제거
                if (localEntry.isExpired()) {
                    localCache.remove(url)
                } else {
                    return localEntry.preview
                }
            }

            // 2. Redis에서 조회
            val key = generateKey(url)
            val cachedValue = redisTemplate.opsForValue().get(key)

            if (cachedValue != null) {
                try {
                    val preview = objectMapper.readValue(cachedValue, ChatMessageMetadata.UrlPreview::class.java)

                    // 로컬 캐시에 저장 (캐시 크기 관리)
                    if (localCache.size >= LOCAL_CACHE_MAX_SIZE) {
                        cleanupLocalCache()
                    }
                    localCache[url] = CacheEntry(preview)

                    return preview
                } catch (e: Exception) {
                    logger.warn { "URL 미리보기 역직렬화 실패: $url - ${e.message}" }
                    return null
                }
            }
            return null
        } catch (e: Exception) {
            logger.error(e) { "URL 미리보기 조회 중 오류: $url" }
            return null
        }
    }

    /**
     * URL 미리보기를 Redis에 저장합니다.
     * 로컬 캐시와 Redis에 모두 저장합니다.
     *
     * @param url 저장할 URL
     * @param preview URL 미리보기 정보
     */
    override fun cacheUrlPreview(url: String, preview: ChatMessageMetadata.UrlPreview) {
        if (url.isBlank()) {
            return
        }

        try {
            // 1. 로컬 캐시에 저장
            if (localCache.size >= LOCAL_CACHE_MAX_SIZE) {
                cleanupLocalCache()
            }
            localCache[url] = CacheEntry(preview)

            // 2. Redis에 저장
            val key = generateKey(url)
            val value = objectMapper.writeValueAsString(preview)

            redisTemplate.opsForValue().set(key, value, CACHE_TTL)
        } catch (e: Exception) {
            logger.error(e) { "URL 미리보기 저장 중 오류: $url - ${e.message}" }
        }
    }

    /**
     * 여러 URL 미리보기를 일괄 저장합니다.
     * 대량의 URL 미리보기를 저장할 때 성능을 향상시킵니다.
     *
     * @param urlPreviewMap URL과 미리보기 정보의 맵
     */
    fun batchCacheUrlPreviews(urlPreviewMap: Map<String, ChatMessageMetadata.UrlPreview>) {
        if (urlPreviewMap.isEmpty()) {
            return
        }

        try {
            // 로컬 캐시에 저장
            urlPreviewMap.forEach { (url, preview) ->
                localCache[url] = CacheEntry(preview)
            }

            // 캐시 크기 관리
            if (localCache.size >= LOCAL_CACHE_MAX_SIZE) {
                cleanupLocalCache()
            }

            // 배치 크기로 나누어 처리
            urlPreviewMap.entries.chunked(BATCH_MAX_SIZE).forEach { chunk ->
                val keyValueMap = chunk.associate { (url, preview) ->
                    generateKey(url) to objectMapper.writeValueAsString(preview)
                }

                // Redis에 일괄 저장
                redisTemplate.opsForValue().multiSet(keyValueMap)

                // 만료 시간 설정
                keyValueMap.keys.forEach { key ->
                    redisTemplate.expire(key, CACHE_TTL)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "URL 미리보기 일괄 저장 중 오류: ${e.message}" }
        }
    }

    /**
     * URL에 대한 캐시 키를 생성합니다.
     *
     * @param url URL
     * @return 캐시 키
     */
    private fun generateKey(url: String): String {
        return redisUtilService.createHashKey(CACHE_PREFIX, url)
    }

    /**
     * 로컬 캐시에서 만료된 항목을 제거합니다.
     */
    private fun cleanupLocalCache() {
        val currentTime = System.currentTimeMillis()

        // 만료된 항목 제거
        val expiredEntries = localCache.entries.filter {
            currentTime - it.value.timestamp > LOCAL_CACHE_TTL_MS
        }

        expiredEntries.forEach { localCache.remove(it.key) }

        // 만료된 항목을 제거해도 여전히 크기가 크면 가장 오래된 항목부터 제거
        if (localCache.size >= LOCAL_CACHE_MAX_SIZE) {
            val oldestEntries = localCache.entries
                .sortedBy { it.value.timestamp }
                .take((LOCAL_CACHE_MAX_SIZE * 0.2).toInt()) // 20% 제거

            oldestEntries.forEach { localCache.remove(it.key) }
        }
    }
}
