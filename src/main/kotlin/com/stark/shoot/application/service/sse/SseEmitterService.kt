package com.stark.shoot.application.service.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.event.FriendAddedEvent
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@UseCase
class SseEmitterService : SseEmitterUseCase {

    private val logger = KotlinLogging.logger {}

    // 타임아웃 설정 - 12시간 (밀리초)
    private val emitterTimeout = 12 * 60 * 60 * 1000L

    // SSE 이미터 및 메타데이터 저장 클래스
    private data class EmitterData(
        val emitter: SseEmitter,
        val createdAt: Instant = Instant.now(),
        val scheduler: ScheduledExecutorService? = null,
        var lastEventTime: Instant = Instant.now()
    )

    // 활성 이미터 저장소
    private val emitters = ConcurrentHashMap<Long, EmitterData>()

    /**
     * 새로운 SSE 이미터 생성
     */
    override fun createEmitter(userId: Long): SseEmitter {
        logger.info { "Creating new SSE emitter for user: $userId" }

        // 기존 이미터 정리
        cleanupEmitter(userId)

        // 타임아웃 설정 - 무한 타임아웃 대신 적절한 시간 설정
        val emitter = SseEmitter(emitterTimeout)

        // 하트비트용 스케줄러 생성
        val scheduler = createHeartbeatScheduler(userId)

        // 이미터 데이터 저장
        val emitterData = EmitterData(
            emitter = emitter,
            scheduler = scheduler
        )
        emitters[userId] = emitterData

        // 이벤트 리스너 설정
        setupEmitterListeners(userId, emitter)

        // 초기 연결 이벤트 전송
        sendConnectedEvent(emitter)

        return emitter
    }

    /**
     * 이미터 이벤트 리스너 설정
     */
    private fun setupEmitterListeners(userId: Long, emitter: SseEmitter) {
        // 완료 리스너
        emitter.onCompletion {
            logger.debug { "SSE connection completed for user: $userId" }
            cleanupEmitter(userId)
        }

        // 타임아웃 리스너
        emitter.onTimeout {
            logger.debug { "SSE connection timeout for user: $userId" }
            cleanupEmitter(userId)
        }

        // 에러 리스너
        emitter.onError { error ->
            if (error.message?.contains("disconnected client") == true ||
                error.message?.contains("Broken pipe") == true
            ) {
                logger.debug { "Client disconnected (normal): $userId" }
            } else {
                logger.error { "SSE connection error for user: $userId, error: ${error.message}" }
            }
            cleanupEmitter(userId)
        }
    }

    /**
     * 초기 연결 이벤트 전송
     */
    private fun sendConnectedEvent(emitter: SseEmitter) {
        try {
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established")
                    .id(System.currentTimeMillis().toString())
            )
        } catch (e: Exception) {
            logger.warn { "Failed to send initial connection event: ${e.message}" }
            // 에러가 발생해도 연결은 유지 (이후 이벤트로 복구 가능)
        }
    }

    /**
     * 하트비트 스케줄러 생성
     */
    private fun createHeartbeatScheduler(userId: Long): ScheduledExecutorService {
        val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
            val thread = Thread(runnable, "sse-heartbeat-$userId")
            thread.isDaemon = true
            thread
        }

        // 15초마다 하트비트 전송
        scheduler.scheduleAtFixedRate({
            try {
                val emitterData = emitters[userId] ?: return@scheduleAtFixedRate

                emitterData.emitter.send(
                    SseEmitter.event()
                        .name("heartbeat")
                        .data("ping")
                        .id(System.currentTimeMillis().toString())
                )

                // 마지막 이벤트 시간 업데이트
                emitterData.lastEventTime = Instant.now()

                logger.debug { "Heartbeat sent to user: $userId" }
            } catch (e: Exception) {
                if (e.message?.contains("Broken pipe") == true) {
                    logger.debug { "Client disconnected (normal): $userId" }
                } else {
                    logger.warn { "Failed to send heartbeat to user: $userId, error: ${e.message}" }
                }
                cleanupEmitter(userId)
            }
        }, 5, 15, TimeUnit.SECONDS)

        return scheduler
    }

    /**
     * 이미터 자원 정리
     */
    private fun cleanupEmitter(userId: Long) {
        emitters[userId]?.let { emitterData ->
            try {
                // 스케줄러 종료
                emitterData.scheduler?.shutdown()

                // 이미터 완료 처리
                emitterData.emitter.complete()

                logger.debug { "Cleaned up emitter for user: $userId" }
            } catch (e: Exception) {
                logger.error { "Error cleaning up emitter: $userId - ${e.message}" }
            } finally {
                // 맵에서 제거
                emitters.remove(userId)
            }
        }
    }

    /**
     * 주기적으로 오래된 이미터 정리 (5분마다 실행)
     */
    @Scheduled(fixedRate = 300_000)
    fun cleanupStaleEmitters() {
        val now = Instant.now()
        val staleThreshold = now.minusSeconds(3600) // 1시간 동안 이벤트 없으면 정리

        val staleEmitters = emitters.entries
            .filter { (_, data) -> data.lastEventTime.isBefore(staleThreshold) }
            .map { it.key }

        if (staleEmitters.isNotEmpty()) {
            logger.info { "Cleaning up ${staleEmitters.size} stale SSE connections" }
            staleEmitters.forEach { userId ->
                cleanupEmitter(userId)
            }
        }
    }

    /**
     * 사용자에게 채팅방 업데이트 전송
     */
    override fun sendUpdate(userId: Long, roomId: Long, unreadCount: Int, lastMessage: String?) {
        val emitterData = emitters[userId] ?: return

        try {
            val data = mapOf(
                "type" to "chat_update",
                "roomId" to roomId,
                "unreadCount" to unreadCount,
                "lastMessage" to (lastMessage ?: ""),
                "timestamp" to System.currentTimeMillis()
            )

            emitterData.emitter.send(
                SseEmitter.event()
                    .name("update")
                    .data(data)
                    .id(System.currentTimeMillis().toString())
            )

            // 마지막 이벤트 시간 업데이트
            emitterData.lastEventTime = Instant.now()

            logger.debug { "Sent update to user: $userId, roomId: $roomId, unreadCount: $unreadCount" }
        } catch (e: Exception) {
            logger.error { "Failed to send update to user: $userId, error: ${e.message}" }
            cleanupEmitter(userId)
        }
    }

    /**
     * 채팅방 생성 이벤트 전송
     */
    override fun sendChatRoomCreatedEvent(event: ChatRoomCreatedEvent) {
        val userId = event.userId
        val emitterData = emitters[userId] ?: return

        try {
            val data = mapOf(
                "type" to "room_created",
                "roomId" to event.roomId,
                "timestamp" to System.currentTimeMillis()
            )

            emitterData.emitter.send(
                SseEmitter.event()
                    .name("chatRoomCreated")
                    .data(data)
                    .id(System.currentTimeMillis().toString())
            )

            // 마지막 이벤트 시간 업데이트
            emitterData.lastEventTime = Instant.now()

            logger.debug { "Sent chatRoomCreated event to user: $userId, roomId: ${event.roomId}" }
        } catch (e: Exception) {
            logger.error { "Failed to send chatRoomCreated event to user: $userId, error: ${e.message}" }
            cleanupEmitter(userId)
        }
    }

    /**
     * 친구 추가 이벤트 전송
     */
    override fun sendFriendAddedEvent(event: FriendAddedEvent) {
        val userId = event.userId
        val emitterData = emitters[userId] ?: return

        try {
            val data = mapOf(
                "type" to "friend_added",
                "friendId" to event.friendId,
                "timestamp" to System.currentTimeMillis()
            )

            emitterData.emitter.send(
                SseEmitter.event()
                    .name("friendAdded")
                    .data(data)
                    .id(System.currentTimeMillis().toString())
            )

            // 마지막 이벤트 시간 업데이트
            emitterData.lastEventTime = Instant.now()

            logger.debug { "Sent friendAdded event to user: $userId, friendId: ${event.friendId}" }
        } catch (e: Exception) {
            logger.error { "Failed to send friendAdded event to user: $userId, error: ${e.message}" }
            cleanupEmitter(userId)
        }
    }

    /**
     * 애플리케이션 종료 시 모든 리소스 정리
     */
    @PreDestroy
    fun shutdown() {
        logger.info { "Shutting down ${emitters.size} SSE connections" }

        val userIds = emitters.keys.toList()
        userIds.forEach { userId ->
            cleanupEmitter(userId)
        }
    }

}