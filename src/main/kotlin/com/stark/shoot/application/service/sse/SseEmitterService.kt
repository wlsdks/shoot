package com.stark.shoot.application.service.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.*
import com.stark.shoot.domain.user.vo.UserId
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

/**
 * Server-Sent Events (SSE) 서비스 구현
 *
 * 이 서비스는 클라이언트와의 실시간 단방향 통신을 위한 SSE 연결을 관리합니다.
 * 주요 기능:
 * - 사용자별 SSE 연결 생성 및 관리
 * - 채팅방 업데이트, 채팅방 생성, 친구 추가 등의 이벤트 전송
 * - 연결 상태 유지를 위한 하트비트 전송
 * - 오래된 연결 자동 정리
 *
 * 이 구현은 다음과 같은 최적화를 포함합니다:
 * - 사용자별 개별 스케줄러를 통한 하트비트 전송
 * - 연결 오류 시 자동 정리 및 오류 응답
 * - 주기적인 오래된 연결 정리
 */
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
     *
     * 사용자 ID를 기반으로 새로운 SSE 연결을 생성합니다.
     * 이 메서드는 다음 작업을 수행합니다:
     * 1. 기존에 존재하는 동일 사용자의 연결을 정리
     * 2. 적절한 타임아웃으로 새 이미터 생성
     * 3. 하트비트 스케줄러 설정
     * 4. 이벤트 리스너 설정 (완료, 타임아웃, 에러)
     * 5. 초기 연결 이벤트 전송
     *
     * 연결 중 오류가 발생하면 오류 정보를 포함한 짧은 타임아웃의 이미터를 반환합니다.
     */
    override fun createEmitter(command: CreateEmitterCommand): SseEmitter {
        val userId = command.userId
        return try {
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
            emitters[userId.value] = emitterData

            // 이벤트 리스너 설정
            setupEmitterListeners(userId, emitter)

            // 초기 연결 이벤트 전송
            sendConnectedEvent(emitter)

            emitter
        } catch (e: Exception) {
            logger.error(e) { "SSE 연결 실패: ${userId.value} - ${e.message}" }
            sendErrorResponse(e, userId)
        }
    }

    /**
     * 이미터 이벤트 리스너 설정
     */
    private fun setupEmitterListeners(
        userId: UserId,
        emitter: SseEmitter
    ) {
        // 완료 리스너
        emitter.onCompletion {
            cleanupEmitter(userId)
        }

        // 타임아웃 리스너
        emitter.onTimeout {
            cleanupEmitter(userId)
        }

        // 에러 리스너
        emitter.onError { error ->
            if (error.message?.contains("disconnected client") == true ||
                error.message?.contains("Broken pipe") == true
            )
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
    private fun createHeartbeatScheduler(userId: UserId): ScheduledExecutorService {
        // 사용자 ID를 Long으로 변환
        val userId = userId.value

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
                cleanupEmitter(UserId.from(userId))
            }
        }, 5, 15, TimeUnit.SECONDS)

        return scheduler
    }

    /**
     * 이미터 자원 정리
     */
    private fun cleanupEmitter(userId: UserId) {
        // 사용자 ID를 Long으로 변환
        val userId = userId.value

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
     *
     * 이 메서드는 Spring의 스케줄링 기능을 사용하여 5분마다 자동으로 실행됩니다.
     * 오래된 SSE 연결을 감지하고 정리하여 리소스 누수를 방지합니다.
     *
     * 다음 기준으로 오래된 연결을 판단합니다:
     * - 마지막 이벤트 시간이 1시간 이상 경과한 연결
     *
     * 이 메커니즘은 클라이언트가 연결을 명시적으로 종료하지 않는 경우에도
     * 서버 측에서 리소스를 적절히 정리할 수 있도록 합니다.
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
                cleanupEmitter(UserId.from(userId))
            }
        }
    }

    /**
     * 공통 이벤트 전송 로직
     *
     * 모든 이벤트 전송에 사용되는 공통 로직을 구현한 헬퍼 메서드입니다.
     * 이 메서드는 다음 작업을 수행합니다:
     * 1. 사용자 ID에 해당하는 이미터 조회
     * 2. 이벤트 데이터와 함께 이벤트 전송
     * 3. 마지막 이벤트 시간 업데이트
     * 4. 로깅
     *
     * 이벤트 전송 중 오류가 발생하면 로깅 후 해당 이미터를 정리합니다.
     * 이 메서드를 사용하면 각 이벤트 전송 메서드에서 중복 코드를 줄일 수 있습니다.
     */
    private fun sendEvent(
        userId: UserId,
        eventName: String,
        data: Map<String, Any>,
        logMessage: String
    ) {
        // 사용자 ID를 Long으로 변환
        val userId = userId.value

        // 이미터 데이터 조회
        val emitterData = emitters[userId] ?: return

        try {
            emitterData.emitter.send(
                SseEmitter.event()
                    .name(eventName)
                    .data(data)
                    .id(System.currentTimeMillis().toString())
            )

            // 마지막 이벤트 시간 업데이트
            emitterData.lastEventTime = Instant.now()

            logger.debug { logMessage }
        } catch (e: Exception) {
            logger.error { "Failed to send $eventName event to user: $userId, error: ${e.message}" }
            cleanupEmitter(UserId.from(userId))
        }
    }

    /**
     * 사용자에게 채팅방 업데이트 전송
     */
    override fun sendUpdate(command: SendUpdateCommand) {
        val userId = command.userId
        val roomId = command.roomId
        val unreadCount = command.unreadCount
        val lastMessage = command.lastMessage

        val data = mapOf(
            "type" to "chat_update",
            "roomId" to roomId.value,
            "unreadCount" to unreadCount,
            "lastMessage" to (lastMessage ?: ""),
            "timestamp" to System.currentTimeMillis()
        )

        sendEvent(
            userId = userId,
            eventName = "update",
            data = data,
            logMessage = "Sent update to user: ${userId.value}, roomId: ${roomId.value}, unreadCount: $unreadCount"
        )
    }

    /**
     * 채팅방 생성 이벤트 전송
     */
    override fun sendChatRoomCreatedEvent(command: SendChatRoomCreatedEventCommand) {
        val event = command.event
        val userId = event.userId

        val data = mapOf(
            "type" to "room_created",
            "roomId" to event.roomId.value,
            "timestamp" to System.currentTimeMillis()
        )

        sendEvent(
            userId = userId,
            eventName = "chatRoomCreated",
            data = data,
            logMessage = "Sent chatRoomCreated event to user: ${userId.value}, roomId: ${event.roomId.value}"
        )
    }

    /**
     * 친구 추가 이벤트 전송
     */
    override fun sendFriendAddedEvent(command: SendFriendAddedEventCommand) {
        val event = command.event
        val userId = event.userId

        val data = mapOf(
            "type" to "friend_added",
            "friendId" to event.friendId.value,
            "timestamp" to System.currentTimeMillis()
        )

        sendEvent(
            userId = userId,
            eventName = "friendAdded",
            data = data,
            logMessage = "Sent friendAdded event to user: ${userId.value}, friendId: ${event.friendId.value}"
        )
    }

    /**
     * 친구 삭제 이벤트 전송
     */
    override fun sendFriendRemovedEvent(command: SendFriendRemovedEventCommand) {
        val event = command.event
        val userId = event.userId

        val data = mapOf(
            "type" to "friend_removed",
            "friendId" to event.friendId.value,
            "timestamp" to System.currentTimeMillis()
        )

        sendEvent(
            userId = userId,
            eventName = "friendRemoved",
            data = data,
            logMessage = "Sent friendRemoved event to user: ${userId.value}, friendId: ${event.friendId.value}"
        )
    }

    /**
     * 애플리케이션 종료 시 모든 리소스 정리
     */
    @PreDestroy
    fun shutdown() {
        logger.info { "Shutting down ${emitters.size} SSE connections" }

        val userIds = emitters.keys.toList()
        userIds.forEach { userId ->
            cleanupEmitter(UserId.from(userId))
        }
    }

    /**
     * SSE 연결 도중 예외 발생 시 에러 전용 SSE 이미터 반환
     * 예외 유형에 따라 다른 오류 메시지를 제공합니다.
     *
     * @param e 예외
     * @param userId 사용자 ID
     * @return 에러 전용 SSE 이미터
     */
    private fun sendErrorResponse(
        e: Exception,
        userId: UserId
    ): SseEmitter {
        // 예외 유형에 따른 더 구체적인 오류 메시지 생성
        val errorType = when {
            e.message?.contains("timeout") == true -> "connection_timeout"
            e.message?.contains("rejected") == true -> "connection_rejected"
            else -> "connection_error"
        }

        val errorMessage = when (errorType) {
            "connection_timeout" -> "연결 시간이 초과되었습니다. 다시 연결하세요."
            "connection_rejected" -> "연결이 거부되었습니다. 잠시 후 다시 시도하세요."
            else -> "연결 오류가 발생했습니다, 다시 연결하세요."
        }

        // 구조화된 로깅으로 더 많은 컨텍스트 제공
        logger.error(e) {
            "SSE 연결 오류 - 유형: $errorType, 사용자: ${userId.value}, 원인: ${e.message ?: "알 수 없음"}"
        }

        val errorEmitter = SseEmitter(3000L) // 짧은 타임아웃
        errorEmitter.send(
            SseEmitter.event()
                .name("error")
                .data("{\"type\":\"$errorType\",\"message\":\"$errorMessage\"}")
        )
        errorEmitter.complete()
        return errorEmitter
    }

}
