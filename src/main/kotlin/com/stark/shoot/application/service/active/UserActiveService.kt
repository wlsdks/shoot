package com.stark.shoot.application.service.active

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.active.ChatActivity
import com.stark.shoot.application.port.`in`.active.UserActiveUseCase
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.redis.RedisUtilService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@UseCase
class UserActiveService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val redisUtilService: RedisUtilService,
    @Value("\${app.user-active.debounce-time-ms:30000}") private val debounceTimeMs: Long = 30000,
    @Value("\${app.user-active.redis-ttl-seconds:45}") private val redisTtlSeconds: Long = 45,
    @Value("\${app.user-active.cleanup-interval-ms:3600000}") private val cleanupIntervalMs: Long = 3600000,
    @Value("\${app.user-active.cache-expiration-hours:24}") private val cacheExpirationHours: Long = 24
) : UserActiveUseCase {

    private val logger = KotlinLogging.logger {}
    private val userActiveCache = ConcurrentHashMap<String, Pair<Boolean, Long>>()

    companion object {
        private const val REDIS_KEY_PREFIX = "active"
    }

    /**
     * 사용자 활동 상태 업데이트
     * 1. 사용자 활동 상태(active)와 마지막 업데이트 시간을 캐시에 저장합니다.
     * 2. 같은 사용자의 같은 상태(active=true 또는 false)에 대한 업데이트가 설정된 시간(기본 30초) 이내에 또 들어오면 무시합니다.
     * 3. 상태가 변경되거나 설정된 시간이 지났을 때만 실제로 Redis를 업데이트하고 로그를 남깁니다.
     *
     * @param message 사용자 활동 메시지 (JSON 형식)
     */
    override fun updateUserActive(message: String) {
        try {
            val activity = parseActivityMessage(message)
            val key = generateRedisKey(activity.userId, activity.roomId)
            val currentTime = System.currentTimeMillis()

            // 디바운싱: 마지막 업데이트와 상태가 같고, 설정된 시간 이내라면 무시
            val lastUpdate = userActiveCache[key]
            if (shouldSkipUpdate(lastUpdate, activity.active, currentTime)) {
                logger.debug { "Skipping duplicate activity update: $key -> ${activity.active}" }
                return
            }

            // 캐시 업데이트
            userActiveCache[key] = Pair(activity.active, currentTime)

            // Redis에 사용자 활동 상태 저장
            updateRedisStatus(key, activity.active)

            logger.debug { "User activity updated: userId=${activity.userId}, roomId=${activity.roomId}, active=${activity.active}" }
        } catch (e: JsonProcessingException) {
            logger.error(e) { "Failed to parse chat activity message: $message" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to process chat activity: $message" }
        }
    }

    /**
     * 활동 메시지를 파싱합니다.
     *
     * @param message JSON 형식의 메시지
     * @return 파싱된 ChatActivity 객체
     * @throws JsonProcessingException JSON 파싱 실패 시
     */
    private fun parseActivityMessage(message: String): ChatActivity {
        return objectMapper.readValue(message, ChatActivity::class.java)
    }

    /**
     * Redis 키를 생성합니다.
     * RedisUtilService의 createKey 메서드를 사용하여 키를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @return 생성된 Redis 키
     */
    private fun generateRedisKey(userId: Long, roomId: Long): String {
        return redisUtilService.createKey(REDIS_KEY_PREFIX, userId.toString(), roomId.toString())
    }

    /**
     * 업데이트를 건너뛸지 결정합니다.
     *
     * @param lastUpdate 마지막 업데이트 정보
     * @param currentActive 현재 활동 상태
     * @param currentTime 현재 시간
     * @return 업데이트를 건너뛸지 여부
     */
    private fun shouldSkipUpdate(
        lastUpdate: Pair<Boolean, Long>?,
        currentActive: Boolean,
        currentTime: Long
    ): Boolean {
        return lastUpdate != null &&
                lastUpdate.first == currentActive &&
                currentTime - lastUpdate.second < debounceTimeMs
    }

    /**
     * Redis에 사용자 활동 상태를 업데이트합니다.
     *
     * @param key Redis 키
     * @param active 활동 상태
     */
    private fun updateRedisStatus(key: String, active: Boolean) {
        redisTemplate.opsForValue().set(key, active.toString(), redisTtlSeconds, TimeUnit.SECONDS)
    }

    /**
     * 만료된 캐시 항목 정리
     * 설정된 간격(기본 1시간)마다 실행되며, 설정된 시간(기본 24시간) 이상 사용되지 않은 캐시 항목을 삭제합니다.
     */
    @Scheduled(fixedRateString = "\${app.user-active.cleanup-interval-ms:3600000}")
    fun cleanupExpiredCacheEntries() {
        val currentTime = Instant.now().toEpochMilli()
        val expirationThreshold = cacheExpirationHours * 3600 * 1000L

        // 삭제 전 캐시 크기 기록
        val beforeSize = userActiveCache.size

        // ConcurrentHashMap의 removeIf 메서드 활용
        userActiveCache.entries.removeIf { (_, value) ->
            currentTime - value.second > expirationThreshold
        }

        // 삭제된 항목 수 계산
        val removedCount = beforeSize - userActiveCache.size

        logger.info { "캐시 정리 완료: ${removedCount}개 항목 제거됨 (현재 캐시 크기: ${userActiveCache.size})" }
    }
}
