package com.stark.shoot.infrastructure.config.socket.interceptor

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.room.vo.ChatRoomId
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.infrastructure.config.socket.StompPrincipal
import com.stark.shoot.infrastructure.exception.web.WebSocketException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.core.Authentication
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

/**
 * StompChannelInterceptor는 STOMP 프로토콜 기반의 메시지를 가로채어
 * 경로 접근 권한을 확인하고, 필요 시 예외를 발생시켜 메시지 처리를 중단하는 역할을 담당합니다.
 *
 * 메시지가 인바운드 채널(서버로 들어오는 경로)을 통과할 때 preSend()가 호출되어
 * STOMP 명령어(SEND, SUBSCRIBE 등)를 확인하고 권한 로직을 수행할 수 있습니다.
 */
class StompChannelInterceptor(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val findUserPort: FindUserPort,
    private val objectMapper: ObjectMapper
) : ChannelInterceptor {

    private val logger = KotlinLogging.logger {}

    // 성능 메트릭
    private val messageCounter = AtomicLong(0)
    private val errorCounter = AtomicLong(0)
    private val processingTimeNanos = AtomicLong(0)
    private var lastMetricLogTime = System.currentTimeMillis()

    // 간단한 캐시 구현 (ConcurrentHashMap 기반)
    private data class CacheEntry(val value: Boolean, val expiry: Instant)

    // 사용자 캐시 - 자주 접근하는 사용자 정보를 캐싱 (10분 만료)
    private val userCache = ConcurrentHashMap<String, CacheEntry>()
    private val userCacheLock = ReentrantReadWriteLock()

    // 채팅방 캐시 - 자주 접근하는 채팅방 정보를 캐싱 (5분 만료)
    private val roomCache = ConcurrentHashMap<String, CacheEntry>()
    private val roomCacheLock = ReentrantReadWriteLock()

    // 캐시 관리 메서드
    private fun <K> ConcurrentHashMap<K, CacheEntry>.getIfPresent(key: K): Boolean? {
        val entry = this[key] ?: return null

        // 만료된 항목 제거
        if (entry.expiry.isBefore(Instant.now())) {
            this.remove(key)
            return null
        }

        return entry.value
    }

    private fun <K> ConcurrentHashMap<K, CacheEntry>.put(key: K, value: Boolean, minutes: Long = 5) {
        val expiry = Instant.now().plusSeconds(minutes * 60)
        this[key] = CacheEntry(value, expiry)
    }

    // 주기적으로 만료된 캐시 항목 정리
    private fun cleanupCaches() {
        val now = Instant.now()

        userCacheLock.write {
            userCache.entries.removeIf { it.value.expiry.isBefore(now) }
        }

        roomCacheLock.write {
            roomCache.entries.removeIf { it.value.expiry.isBefore(now) }
        }
    }

    /**
     * STOMP 프레임이 인바운드 채널을 통과하기 전에 호출됩니다.
     * 메시지의 STOMP 명령어와 헤더를 확인하여 특정 경로의 접근 권한을 검사할 수 있습니다.
     * 성능 최적화를 위해 캐싱과 메트릭 수집을 추가했습니다.
     */
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val startTime = System.nanoTime()
        val messageId = messageCounter.incrementAndGet()

        try {
            val accessor = StompHeaderAccessor.wrap(message)
            val command = accessor.command ?: return message
            val destination = accessor.destination

            // 메트릭 로깅 (1초에 한 번)
            logMetricsIfNeeded()

            // SockJS fallback 경로는 인증 생략 (최적화: 빠른 경로 반환)
            val path = accessor.sessionAttributes?.get("sockJsPath") as? String ?: ""
            if (path.contains("/xhr_send") || path.contains("/xhr_streaming") || path.endsWith("/info")) {
                return message
            }

            // 인증 정보 확인 및 복구 (캐싱 적용)
            if (accessor.user == null) {
                val auth = accessor.sessionAttributes?.get("authentication") as? Authentication
                if (auth != null) {
                    // 사용자 캐시 확인
                    val userId = auth.name
                    if (userCache.getIfPresent(userId) == null) {
                        // 캐시에 없으면 사용자 존재 여부 확인 후 캐싱
                        try {
                            // userId가 Long으로 변환 가능한지 확인
                            val userIdLong = userId.toLongOrNull()
                            if (userIdLong != null && findUserPort.existsById(UserId.from(userIdLong))) {
                                userCache.put(userId, true, 10) // 10분 캐싱
                            }
                        } catch (e: Exception) {
                            logger.warn(e) { "User validation failed: $userId" }
                        }
                    }

                    accessor.user = StompPrincipal(userId)
                    logger.debug { "Restored authentication for user: $userId" }
                } else {
                    logger.error { "No authentication found for message: $destination" }
                    errorCounter.incrementAndGet()
                    throw WebSocketException("Authentication required")
                }
            }

            // SEND 명령어 처리 (최적화: 명령어별 분기 처리)
            if (command == StompCommand.SEND) {
                when (destination) {
                    "/app/chat" -> {
                        val parsedMessage = getChatMessage(message) ?: return message

                        // 채팅방 캐시 확인
                        val roomId = parsedMessage.roomId
                        val roomIdStr = roomId.toString()
                        if (roomCache.getIfPresent(roomIdStr) == null) {
                            // 캐시에 없으면 채팅방 존재 여부 확인 후 캐싱
                            try {
                                // roomId는 이미 Long 타입이므로 직접 사용
                                val room = loadChatRoomPort.findById(ChatRoomId.from(roomId))
                                if (room != null) {
                                    roomCache.put(roomIdStr, true, 5) // 5분 캐싱
                                } else {
                                    throw WebSocketException("Chat room not found: $roomId")
                                }
                            } catch (e: Exception) {
                                logger.warn(e) { "Chat room validation failed: $roomId" }
                                errorCounter.incrementAndGet()
                                throw WebSocketException("Invalid chat room: ${e.message}")
                            }
                        }

                        validateMessage(parsedMessage)
                    }

                    // 기타 경로 처리 (최소화된 검증)
                    "/app/active", "/app/typing", "/app/read-all", "/app/sync" -> {
                        getRawPayload(message)
                    }

                    else -> {
                        logger.warn { "Unknown destination: $destination" }
                    }
                }
            }

            return message
        } catch (e: Exception) {
            errorCounter.incrementAndGet()
            throw e
        } finally {
            // 처리 시간 측정 및 누적
            val processingTime = System.nanoTime() - startTime
            processingTimeNanos.addAndGet(processingTime)

            // 상세 디버깅 (1000번째 메시지마다)
            if (messageId % 1000 == 0L) {
                logger.debug { "WebSocket message #$messageId processed in ${processingTime / 1_000_000.0} ms" }
            }
        }
    }

    /**
     * 메트릭 로깅 (1초에 한 번)
     */
    private fun logMetricsIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMetricLogTime > 1000) {
            val messageCount = messageCounter.get()
            val errorCount = errorCounter.get()
            val avgProcessingTime = if (messageCount > 0)
                processingTimeNanos.get() / messageCount / 1_000_000.0 else 0.0

            logger.debug { "WebSocket metrics: messages=$messageCount, errors=$errorCount, avg_time=${avgProcessingTime}ms" }
            lastMetricLogTime = currentTime
        }
    }

    /**
     * 메시지 페이로드에서 ChatMessageRequest 객체 추출 (최적화: 예외 처리 개선)
     */
    private fun getChatMessage(message: Message<*>): ChatMessageRequest? {
        val payload = getRawPayload(message) ?: return null
        return try {
            // 최적화: 직렬화 성능 향상을 위해 readValue 사용
            objectMapper.readValue(payload, ChatMessageRequest::class.java)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse chat message: ${payload.take(100)}${if (payload.length > 100) "..." else ""}" }
            throw WebSocketException("Failed to parse chat message: ${e.message}")
        }
    }

    /**
     * 메시지에서 원시 페이로드 추출 (최적화: 타입 체크 개선)
     */
    private fun getRawPayload(message: Message<*>): String? {
        val payload = message.payload ?: return null

        return when (payload) {
            is String -> payload.ifBlank { null }
            is ByteArray -> if (payload.isEmpty()) null else String(payload)
            else -> throw WebSocketException("Unsupported message payload type: ${payload.javaClass.simpleName}")
        }
    }

    /**
     * ChatMessageRequest 유효성 검사 (최적화: 검증 로직 개선)
     */
    private fun validateMessage(message: ChatMessageRequest) {
        // content가 null인지 확인
        if (message.content == null) {
            throw WebSocketException("Message content cannot be null")
        }

        // text가 null인지 확인 (MessageContentRequest에서 text는 non-nullable이지만 JSON 역직렬화 과정에서 null이 될 수 있음)
        val text = message.content.text ?: throw WebSocketException("Message text cannot be null")

        // 텍스트 길이 검증
        val textLength = text.length
        if (textLength > 1000) {
            throw WebSocketException("Message content too long: $textLength characters (max: 1000)")
        }

        // 추가 검증 로직 (필요시 구현)
    }
}
