package com.stark.shoot.infrastructure.config.redis

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Redis 관련 공통 유틸리티 서비스
 *
 * 이 서비스는 Redis 작업을 중앙 집중화하여 코드 중복을 줄이고 일관된 오류 처리를 제공합니다.
 * Redis 작업 실패 시 로컬 캐시를 폴백으로 사용할 수 있는 기능을 포함합니다.
 * 다양한 서비스에서 사용할 수 있는 키 생성 유틸리티 메서드를 제공합니다.
 */
@Service
class RedisUtilService(
    private val redisTemplate: StringRedisTemplate
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        // 로컬 캐시 최대 보관 시간 (밀리초)
        private const val DEFAULT_CACHE_MAX_AGE_MS = 30_000L
    }

    // 로컬 캐시 (Redis 장애 시 폴백으로 사용)
    private val localCache = ConcurrentHashMap<String, CacheEntry>()

    /**
     * Redis에서 값을 안전하게 가져오는 메서드
     * Redis 작업 실패 시 기본값 또는 로컬 캐시 값을 반환합니다.
     *
     * @param key Redis 키
     * @param defaultValue 기본값 (Redis에 값이 없거나 오류 발생 시 반환)
     * @param useLocalCache 로컬 캐시 사용 여부
     * @return Redis에서 조회한 값 또는 기본값
     */
    fun getValueSafely(
        key: String,
        defaultValue: String = "",
        useLocalCache: Boolean = true
    ): String {
        return try {
            // Redis에서 값 조회
            val value = redisTemplate.opsForValue().get(key)

            if (value != null) {
                // 값이 있으면 로컬 캐시 업데이트
                if (useLocalCache) {
                    localCache[key] = CacheEntry(value, System.currentTimeMillis())
                }
                value
            } else {
                // Redis에 값이 없으면 로컬 캐시 확인
                if (useLocalCache) {
                    val cachedValue = localCache[key]?.value
                    cachedValue ?: defaultValue
                } else {
                    defaultValue
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Redis에서 값 조회 실패: $key" }

            // 오류 발생 시 로컬 캐시 확인
            if (useLocalCache) {
                localCache[key]?.value ?: defaultValue
            } else {
                defaultValue
            }
        }
    }

    /**
     * Redis에 값을 안전하게 설정하는 메서드
     * Redis 작업 실패 시 로컬 캐시에 값을 저장합니다.
     *
     * @param key Redis 키
     * @param value 저장할 값
     * @param expiry 만료 시간
     * @param useLocalCache 로컬 캐시 사용 여부
     * @return 작업 성공 여부
     */
    fun setValueSafely(
        key: String,
        value: String,
        expiry: Duration? = null,
        useLocalCache: Boolean = true
    ): Boolean {
        return try {
            if (expiry != null) {
                redisTemplate.opsForValue().set(key, value, expiry)
            } else {
                redisTemplate.opsForValue().set(key, value)
            }

            // 로컬 캐시 업데이트
            updateLocalCache(key, value, useLocalCache)

            true
        } catch (e: Exception) {
            logger.warn(e) { "Redis에 값 저장 실패: $key" }

            // 오류 발생 시 로컬 캐시에만 저장
            updateLocalCache(key, value, useLocalCache)

            false
        }
    }

    /**
     * 로컬 캐시를 업데이트하는 private 메서드
     * 
     * @param key 캐시 키
     * @param value 저장할 값
     * @param useLocalCache 로컬 캐시 사용 여부
     */
    private fun updateLocalCache(key: String, value: String, useLocalCache: Boolean) {
        if (useLocalCache) {
            localCache[key] = CacheEntry(value, System.currentTimeMillis())
        }
    }

    /**
     * Redis에서 키 패턴으로 키 목록을 조회하는 메서드
     *
     * @param pattern 키 패턴 (예: "user:*")
     * @return 패턴과 일치하는 키 목록 (오류 발생 시 빈 목록)
     */
    fun getKeysByPattern(pattern: String): Set<String> {
        return try {
            redisTemplate.keys(pattern) ?: emptySet()
        } catch (e: Exception) {
            logger.warn(e) { "Redis에서 키 패턴 조회 실패: $pattern" }
            emptySet()
        }
    }

    /**
     * Redis에서 키를 삭제하는 메서드
     *
     * @param key 삭제할 키
     * @param removeFromLocalCache 로컬 캐시에서도 삭제할지 여부
     * @return 삭제 성공 여부
     */
    fun deleteKey(key: String, removeFromLocalCache: Boolean = true): Boolean {
        return try {
            val result = redisTemplate.delete(key) ?: false

            // 로컬 캐시에서도 삭제
            if (removeFromLocalCache) {
                localCache.remove(key)
            }

            result
        } catch (e: Exception) {
            logger.warn(e) { "Redis에서 키 삭제 실패: $key" }

            // 오류 발생 시 로컬 캐시에서만 삭제
            if (removeFromLocalCache) {
                localCache.remove(key)
            }

            false
        }
    }

    /**
     * 로컬 캐시에서 오래된 항목을 정리하는 메서드
     *
     * @param maxAgeMs 최대 보관 시간 (밀리초)
     */
    fun cleanupLocalCache(maxAgeMs: Long = DEFAULT_CACHE_MAX_AGE_MS) {
        try {
            val now = System.currentTimeMillis()
            val expiredKeys = localCache.entries
                .filter { now - it.value.timestamp > maxAgeMs }
                .map { it.key }
                .toList()

            if (expiredKeys.isNotEmpty()) {
                expiredKeys.forEach { localCache.remove(it) }
                logger.debug { "로컬 캐시 정리: ${expiredKeys.size}개 항목 제거됨" }
            }
        } catch (e: Exception) {
            logger.error(e) { "로컬 캐시 정리 중 오류 발생" }
        }
    }

    /**
     * 여러 컴포넌트를 조합하여 Redis 키를 생성하는 메서드
     *
     * @param prefix 키 접두사
     * @param components 키 구성 요소들 (문자열로 변환됨)
     * @return 생성된 Redis 키
     */
    fun createKey(prefix: String, vararg components: Any): String {
        return buildString {
            append(prefix)
            if (!prefix.endsWith(":") && components.isNotEmpty()) {
                append(":")
            }
            append(components.joinToString(":") { it.toString() })
        }
    }

    /**
     * 해시 기반 Redis 키를 생성하는 메서드
     *
     * @param prefix 키 접두사
     * @param value 해시할 값
     * @return 생성된 Redis 키
     */
    fun createHashKey(prefix: String, value: String): String {
        return buildString {
            append(prefix)
            append(value.hashCode())
        }
    }

    /**
     * 상태 키를 생성하는 메서드 (기존 키에 ":state" 추가)
     *
     * @param baseKey 기본 키
     * @param stateSuffix 상태 접미사 (기본값: "state")
     * @return 생성된 상태 키
     */
    fun createStateKey(baseKey: String, stateSuffix: String = "state"): String {
        return buildString {
            append(baseKey)
            append(":")
            append(stateSuffix)
        }
    }

    /**
     * 로컬 캐시 항목 클래스
     */
    private data class CacheEntry(
        val value: String,
        val timestamp: Long
    )

}
