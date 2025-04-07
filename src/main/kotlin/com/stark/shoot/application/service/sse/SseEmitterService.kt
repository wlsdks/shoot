package com.stark.shoot.application.service.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.event.FriendAddedEvent
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@UseCase
class SseEmitterService : SseEmitterUseCase {

    private val emitters = ConcurrentHashMap<Long, SseEmitter>()
    private val logger = KotlinLogging.logger {}

    // 활성 사용자 ID 캐시 추가
    private val activeUserIds = ConcurrentHashMap.newKeySet<Long>()

    // 스케줄러를 추적하기 위한 컬렉션 추가
    private val heartbeatSchedulers = ConcurrentHashMap<Long, ScheduledExecutorService>()

    /**
     * 새로운 SSE emitter를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return SseEmitter
     */
    override fun createEmitter(
        userId: Long
    ): SseEmitter {
        val emitter = SseEmitter(0L) // 무한 타임아웃
        logger.info { "Creating new SSE emitter for user: $userId" }

        // 기존 emitter가 있으면 완료 처리
        emitters[userId]?.complete()

        // 새 emitter 저장
        emitters[userId] = emitter

        // 활성 사용자로 등록
        activeUserIds.add(userId)

        // 완료 콜백 설정
        emitter.onCompletion {
            logger.debug { "SSE connection completed for user: $userId" }
            cleanupEmitter(userId)
        }

        // 타임아웃 콜백 설정
        emitter.onTimeout {
            logger.debug { "SSE connection timeout for user: $userId" }
            cleanupEmitter(userId)
        }

        // 에러 콜백 설정
        emitter.onError { error ->
            // 일반적인 클라이언트 연결 끊김은 DEBUG로 로깅
            if (error.message?.contains("disconnected client") == true ||
                error.message?.contains("Broken pipe") == true
            ) {
                logger.debug { "Client disconnected (normal): $userId - ${error.message}" }
            } else {
                logger.error { "SSE connection error for user: $userId, error: ${error.message}" }
            }
            cleanupEmitter(userId)
        }

        // 연결 성공 이벤트 전송
        try {
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established")
                    .id(System.currentTimeMillis().toString())
            )
        } catch (e: Exception) {
            logger.warn { "Failed to send initial connection event: $userId - ${e.message}" }
        }

        // Heartbeat 전송 시작
        startHeartbeat(emitter, userId)

        return emitter
    }

    /**
     * 사용자에게 채팅방 업데이트를 전송합니다.
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @param unreadCount 읽지 않은 메시지 수
     * @param lastMessage 마지막 메시지
     */
    override fun sendUpdate(
        userId: Long,
        roomId: Long,
        unreadCount: Int,
        lastMessage: String?
    ) {
        // 활성 사용자가 아닌 경우 무시
        if (!isActiveUser(userId)) {
            return
        }

        val emitter = emitters[userId] ?: return

        try {
            // 데이터 구조를 명확하게 하여 JSON 형식으로 보냄
            val data = mapOf(
                "type" to "chat_update",
                "roomId" to roomId,
                "unreadCount" to unreadCount,
                "lastMessage" to (lastMessage ?: ""),
                "timestamp" to System.currentTimeMillis()
            )

            emitter.send(
                SseEmitter.event()
                    .name("update")
                    .data(data)
                    .id(System.currentTimeMillis().toString())
            )

            logger.debug { "Sent update to user: $userId, roomId: $roomId, unreadCount: $unreadCount" }
        } catch (e: Exception) {
            logger.error { "Failed to send update to user: $userId, error: ${e.message}" }
            cleanupEmitter(userId)
        }
    }

    /**
     * 사용자에게 채팅방 생성 이벤트를 전송합니다.
     *
     * @param event 이벤트
     */
    override fun sendChatRoomCreatedEvent(event: ChatRoomCreatedEvent) {
        // 이벤트에서 userId와 roomId 추출 (String에서 Long으로 변환)
        val userId = event.userId
        val roomId = event.roomId

        // 활성 사용자가 아닌 경우 무시
        if (!isActiveUser(userId)) {
            return
        }

        val emitter = emitters[userId] ?: return

        try {
            val data = mapOf(
                "type" to "room_created",
                "roomId" to roomId,
                "timestamp" to System.currentTimeMillis()
            )

            emitter.send(
                SseEmitter.event()
                    .name("chatRoomCreated")
                    .data(data)
                    .id(System.currentTimeMillis().toString())
            )

            logger.debug { "Sent chatRoomCreated event to user: $userId, roomId: $roomId" }
        } catch (e: Exception) {
            logger.error { "Failed to send chatRoomCreated event to user: $userId, error: ${e.message}" }
            cleanupEmitter(userId)
        }
    }

    /**
     * 사용자에게 친구 추가 이벤트를 전송합니다.
     *
     * @param event 이벤트
     */
    override fun sendFriendAddedEvent(event: FriendAddedEvent) {
        // 이벤트에서 userId와 friendId 추출 (String에서 Long으로 변환)
        val userId = event.userId
        val friendId = event.friendId

        // 활성 사용자가 아닌 경우 무시
        if (!isActiveUser(userId)) {
            return
        }

        val emitter = emitters[userId] ?: return

        try {
            val data = mapOf(
                "type" to "friend_added",
                "friendId" to friendId,
                "timestamp" to System.currentTimeMillis()
            )

            emitter.send(
                SseEmitter.event()
                    .name("friendAdded")
                    .data(data)
                    .id(System.currentTimeMillis().toString())
            )

            logger.debug { "Sent friendAdded event to user: $userId, friendId: $friendId" }
        } catch (e: Exception) {
            logger.error { "Failed to send friendAdded event to user: $userId, error: ${e.message}" }
            cleanupEmitter(userId)
        }
    }

    /**
     * 특정 사용자가 활성 상태인지 확인합니다.
     */
    private fun isActiveUser(userId: Long): Boolean {
        return activeUserIds.contains(userId)
    }

    /**
     * emitter 자원 정리
     */
    private fun cleanupEmitter(userId: Long) {
        // emitter 제거
        emitters.remove(userId)

        // 활성 사용자에서 제거
        activeUserIds.remove(userId)

        // heartbeat 스케줄러 중지
        heartbeatSchedulers[userId]?.let {
            it.shutdown()
            heartbeatSchedulers.remove(userId)
            logger.debug { "Stopped heartbeat scheduler for user: $userId" }
        }
    }

    /**
     * SSE emitter에 Heartbeat를 주기적으로 전송합니다.
     *
     * @param emitter SSE emitter
     * @param userId 사용자 ID
     */
    private fun startHeartbeat(emitter: SseEmitter, userId: Long) {
        // 스레드 이름을 직접 설정하는 간단한 방법
        val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
            val thread = Thread(runnable, "sse-heartbeat-$userId")
            thread.isDaemon = true
            thread
        }

        // 스케줄러 추적
        heartbeatSchedulers[userId] = scheduler

        // 15초마다 Heartbeat 전송
        scheduler.scheduleAtFixedRate({
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("heartbeat")
                        .data("ping")
                        .id(System.currentTimeMillis().toString())
                )

                logger.debug { "Heartbeat sent to user: $userId" }
            } catch (e: Exception) {
                // 파이프 끊김 오류는 일반적인 현상이므로 DEBUG 레벨로 로깅
                if (e.message?.contains("Broken pipe") == true) {
                    logger.debug { "Client disconnected (normal): $userId - ${e.message}" }
                } else {
                    logger.warn { "Failed to send heartbeat to user: $userId, error: ${e.message}" }
                }

                cleanupEmitter(userId)
            }
        }, 5, 15, TimeUnit.SECONDS) // 최초 5초 후 시작, 이후 15초마다 반복
    }

    /**
     * 애플리케이션 종료 시 리소스를 정리합니다.
     */
    @PreDestroy
    fun shutdown() {
        logger.info { "Shutting down ${heartbeatSchedulers.size} SSE heartbeat schedulers" }
        heartbeatSchedulers.forEach { (userId, scheduler) ->
            try {
                scheduler.shutdown()
                logger.debug { "Shutdown heartbeat scheduler for user: $userId" }
            } catch (e: Exception) {
                logger.error { "Failed to shutdown heartbeat scheduler for user: $userId - ${e.message}" }
            }
        }
        heartbeatSchedulers.clear()

        // 모든 emitter 완료 처리
        emitters.forEach { (userId, emitter) ->
            try {
                emitter.complete()
                logger.debug { "Completed emitter for user: $userId" }
            } catch (e: Exception) {
                logger.error { "Failed to complete emitter for user: $userId - ${e.message}" }
            }
        }
        emitters.clear()
        activeUserIds.clear()
    }

}