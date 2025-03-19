package com.stark.shoot.application.service.active

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.active.ChatActivity
import com.stark.shoot.application.port.`in`.active.UserActiveUseCase
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@UseCase
class UserActiveService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : UserActiveUseCase {

    private val logger = KotlinLogging.logger {}
    private val userActiveCache = ConcurrentHashMap<String, Pair<Boolean, Long>>()

    /**
     * 사용자 활동 상태 업데이트
     * 1. 사용자 활동 상태(active)와 마지막 업데이트 시간을 캐시에 저장합니다.
     * 2. 같은 사용자의 같은 상태(active=true 또는 false)에 대한 업데이트가 30초 이내에 또 들어오면 무시합니다.
     * 3. 상태가 변경되거나 30초가 지났을 때만 실제로 Redis를 업데이트하고 로그를 남깁니다.
     *
     * @param message 사용자 활동 메시지
     */
    override fun updateUserActive(message: String) {
        try {
            val activity = objectMapper.readValue(message, ChatActivity::class.java)
            val key = "active:${activity.userId}:${activity.roomId}"
            val currentTime = System.currentTimeMillis()

            // 디바운싱: 마지막 업데이트와 상태가 같고, 30초 이내라면 무시
            val lastUpdate = userActiveCache[key]
            if (lastUpdate != null &&
                lastUpdate.first == activity.active &&
                currentTime - lastUpdate.second < 30000
            ) {
                return
            }

            // 캐시 업데이트
            userActiveCache[key] = Pair(activity.active, currentTime)

            // Redis에 사용자 활동 상태 저장
            redisTemplate.opsForValue().set(key, activity.active.toString(), 45, TimeUnit.SECONDS)
            logger.info { "User activity updated: $key -> ${activity.active}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to process chat activity: $message" }
        }
    }


    /**
     * 만료된 캐시 항목 정리
     * 1시간마다 실행되며, 24시간 이상 사용되지 않은 캐시 항목을 삭제합니다.
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    fun cleanupExpiredCacheEntries() {
        val currentTime = Instant.now().toEpochMilli()
        val expirationThreshold = 24 * 3600 * 1000L // 24시간

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