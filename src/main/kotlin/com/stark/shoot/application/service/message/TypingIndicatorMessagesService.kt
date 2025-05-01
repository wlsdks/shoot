package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.adapter.`in`.web.socket.dto.TypingIndicatorMessage
import com.stark.shoot.application.port.`in`.message.TypingIndicatorMessageUseCase
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@UseCase
class TypingIndicatorMessagesService(
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val redisTemplate: StringRedisTemplate,
) : TypingIndicatorMessageUseCase {

    private val logger = KotlinLogging.logger {}

    // 로컬 캐시 (Redis 장애 시 폴백으로 사용)
    private val localTypingCache = ConcurrentHashMap<String, Long>()

    // 타이핑 인디케이터 관련 상수
    companion object {
        private const val RATE_LIMIT_MS = 1000L // 타이핑 인디케이터 전송 제한 시간 (1초)
        private const val ENTRY_EXPIRY_MS = 30_000L // 항목 만료 시간 (30초)
        private const val STOPPED_TYPING_DELAY_MS = 5000L // 타이핑 중지 감지 시간 (5초)
        private const val REDIS_KEY_PREFIX = "typing:" // Redis 키 접두사
    }

    /**
     * Redis 키 생성 유틸리티 메서드
     */
    private fun createRedisKey(
        userId: Long,
        roomId: Long
    ): String = "$REDIS_KEY_PREFIX$userId:$roomId"

    /**
     * Redis 상태 키 생성 유틸리티 메서드
     */
    private fun createRedisStateKey(
        redisKey: String
    ): String = "$redisKey:state"

    /**
     * Redis에서 값을 안전하게 가져오는 유틸리티 메서드
     */
    private fun getRedisValueSafely(
        key: String,
        defaultValue: String = "0"
    ): String {
        return try {
            redisTemplate.opsForValue().get(key) ?: defaultValue
        } catch (e: Exception) {
            logger.warn(e) { "Redis에서 값 조회 실패: $key" }
            defaultValue
        }
    }

    /**
     * Redis에 값을 안전하게 설정하는 유틸리티 메서드
     */
    private fun setRedisValueSafely(
        key: String,
        value: String,
        expiry: Duration = Duration.ofMillis(ENTRY_EXPIRY_MS)
    ): Boolean {
        return try {
            redisTemplate.opsForValue().set(key, value, expiry)
            true
        } catch (e: Exception) {
            logger.warn(e) { "Redis에 값 저장 실패: $key" }
            false
        }
    }

    /**
     * 타이핑 인디케이터 메시지를 전송합니다.
     * 동일한 사용자-채팅방 조합에 대해 RATE_LIMIT_MS 시간 내에 중복 메시지 전송을 방지합니다.
     * 타이핑 상태가 변경되면 즉시 전송합니다.
     *
     * @param message 타이핑 인디케이터 메시지 (사용자 ID, 채팅방 ID, 타이핑 상태 포함)
     */
    override fun sendMessage(message: TypingIndicatorMessage) {
        try {
            val key = "${message.userId}:${message.roomId}"
            val redisKey = createRedisKey(message.userId, message.roomId)
            val stateKey = createRedisStateKey(redisKey)
            val now = System.currentTimeMillis()

            // Redis에서 마지막 전송 시간 조회 (없으면 0)
            val lastSentStr = getRedisValueSafely(redisKey, localTypingCache.getOrDefault(key, 0L).toString())
            val lastSent = lastSentStr.toLongOrNull() ?: 0L

            // 이전 타이핑 상태 확인
            val previousTypingStateStr = getRedisValueSafely(stateKey, "false")
            val previousTypingState = previousTypingStateStr.toBoolean()

            // 타이핑 상태가 변경되었거나 제한 시간이 지났으면 메시지 전송
            if (message.isTyping != previousTypingState || now - lastSent > RATE_LIMIT_MS) {
                logger.debug { "타이핑 인디케이터 전송: 사용자=${message.userId}, 채팅방=${message.roomId}, 상태=${message.isTyping}" }

                webSocketMessageBroker.sendMessage(
                    "/topic/typing/${message.roomId}",
                    message
                ).exceptionally { throwable ->
                    logger.error(throwable) { "타이핑 인디케이터 전송 실패: ${throwable.message}" }
                    null
                }

                // Redis에 마지막 전송 시간과 상태 저장
                val timeUpdated = setRedisValueSafely(redisKey, now.toString())
                val stateUpdated = setRedisValueSafely(stateKey, message.isTyping.toString())

                // Redis 저장 실패 시 로컬 캐시 사용
                if (!timeUpdated) {
                    localTypingCache[key] = now
                }
            } else {
                logger.trace { "타이핑 인디케이터 제한: 사용자=${message.userId}, 채팅방=${message.roomId} (${now - lastSent}ms < ${RATE_LIMIT_MS}ms)" }
            }
        } catch (e: Exception) {
            logger.error(e) { "타이핑 인디케이터 처리 중 오류 발생: ${e.message}" }
        }
    }

    /**
     * 오래된 타이핑 인디케이터 항목을 정리합니다.
     * ENTRY_EXPIRY_MS보다 오래된 항목은 메모리에서 제거됩니다.
     *
     * 이 메서드는 Spring의 스케줄러에 의해 주기적으로 실행됩니다.
     */
    @Scheduled(
        fixedRateString = "\${app.typing-indicator.cleanup-interval-minutes:10}",
        timeUnit = java.util.concurrent.TimeUnit.MINUTES
    )
    fun cleanupOldEntries() {
        try {
            cleanupLocalCache()
            // Redis 캐시는 TTL로 자동 정리되므로 별도 작업 불필요
        } catch (e: Exception) {
            logger.error(e) { "타이핑 인디케이터 캐시 정리 중 오류 발생: ${e.message}" }
        }
    }

    /**
     * 로컬 캐시에서 만료된 항목을 제거합니다.
     */
    private fun cleanupLocalCache() {
        val now = System.currentTimeMillis()
        val expiredLocalKeys = localTypingCache.entries
            .filter { now - it.value > ENTRY_EXPIRY_MS }
            .map { it.key }
            .toList()

        if (expiredLocalKeys.isNotEmpty()) {
            expiredLocalKeys.forEach { localTypingCache.remove(it) }
            logger.debug { "로컬 타이핑 인디케이터 캐시 정리: ${expiredLocalKeys.size}개 항목 제거됨" }
        }
    }

    /**
     * 타이핑 중지 상태를 감지하고 처리합니다.
     * 마지막 타이핑 이벤트 이후 STOPPED_TYPING_DELAY_MS 시간이 지나면 타이핑 중지 이벤트를 발생시킵니다.
     *
     * 이 메서드는 Spring의 스케줄러에 의해 주기적으로 실행됩니다.
     */
    @Scheduled(fixedRate = 1000) // 1초마다 실행
    fun detectStoppedTyping() {
        try {
            val now = System.currentTimeMillis()
            val pattern = "$REDIS_KEY_PREFIX*"

            // Redis에서 모든 타이핑 키 조회
            val keys = try {
                redisTemplate.keys(pattern)
            } catch (e: Exception) {
                logger.warn(e) { "Redis에서 타이핑 키 조회 실패" }
                emptySet<String>()
            }

            // 각 키에 대해 타이핑 중지 상태 확인
            for (redisKey in keys) {
                if (!redisKey.endsWith(":state")) { // 상태 키는 건너뜀
                    processTypingKey(redisKey, now)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "타이핑 중지 감지 처리 중 오류 발생: ${e.message}" }
        }
    }

    /**
     * 개별 타이핑 키를 처리하는 메서드
     */
    private fun processTypingKey(redisKey: String, now: Long) {
        try {
            val stateKey = createRedisStateKey(redisKey)

            // 마지막 타이핑 시간 조회
            val lastTypingStr = getRedisValueSafely(redisKey)
            val lastTyping = lastTypingStr.toLongOrNull() ?: return

            // 타이핑 상태 확인
            val isTypingStr = getRedisValueSafely(stateKey, "false")
            val isTyping = isTypingStr.toBoolean()

            // 타이핑 중이고 마지막 타이핑 이후 일정 시간이 지났으면 중지 이벤트 발생
            if (isTyping && now - lastTyping > STOPPED_TYPING_DELAY_MS) {
                // 키에서 사용자 ID와 채팅방 ID 추출
                val keyParts = redisKey.removePrefix(REDIS_KEY_PREFIX).split(":")
                if (keyParts.size >= 2) {
                    val userId = keyParts[0].toLongOrNull() ?: return
                    val roomId = keyParts[1].toLongOrNull() ?: return

                    sendTypingStoppedMessage(userId, roomId, now - lastTyping)

                    // 상태 업데이트
                    setRedisValueSafely(stateKey, "false")
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "타이핑 중지 감지 중 오류 발생: $redisKey" }
        }
    }

    /**
     * 타이핑 중지 메시지를 전송하는 메서드
     */
    private fun sendTypingStoppedMessage(userId: Long, roomId: Long, inactiveTime: Long) {
        // 타이핑 중지 메시지 생성
        val stoppedMessage = TypingIndicatorMessage(
            roomId = roomId,
            userId = userId,
            isTyping = false
        )

        logger.debug { "타이핑 중지 감지: 사용자=$userId, 채팅방=$roomId (${inactiveTime}ms 동안 타이핑 없음)" }

        webSocketMessageBroker.sendMessage(
            "/topic/typing/$roomId",
            stoppedMessage
        ).exceptionally { throwable ->
            logger.error(throwable) { "타이핑 중지 메시지 전송 실패: ${throwable.message}" }
            null
        }
    }
}
