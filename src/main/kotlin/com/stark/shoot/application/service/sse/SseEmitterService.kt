package com.stark.shoot.application.service.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.event.FriendAddedEvent
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@UseCase
class SseEmitterService : SseEmitterUseCase {

    private val emitters = ConcurrentHashMap<String, SseEmitter>()
    private val logger = KotlinLogging.logger {}

    // 활성 사용자 ID 캐시 추가
    private val activeUserIds = ConcurrentHashMap.newKeySet<String>()

    /**
     * 새로운 SSE emitter를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return SseEmitter
     */
    override fun createEmitter(
        userId: String
    ): SseEmitter {
        val emitter = SseEmitter(0L) // 무한 타임아웃
        logger.info { "Creating new SSE emitter for user: $userId" }
        emitters[userId] = emitter

        // 활성 사용자로 등록
        activeUserIds.add(userId)

        emitter.onCompletion {
            logger.info { "SSE connection completed for user: $userId" }
            emitters.remove(userId)
            activeUserIds.remove(userId) // 활성 사용자에서 제거
        }

        emitter.onTimeout {
            logger.info { "SSE connection timeout for user: $userId" }
            emitters.remove(userId)
            activeUserIds.remove(userId) // 활성 사용자에서 제거
        }

        emitter.onError {
            logger.error { "SSE connection error for user: $userId, error: ${it.message}" }
            emitters.remove(userId)
            activeUserIds.remove(userId) // 활성 사용자에서 제거
        }

        // Heartbeat 전송은 유지
        sendSseHeartBeat(emitter, userId)
        return emitter
    }

    /**
     * 사용자에게 업데이트를 전송합니다.
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @param unreadCount 읽지 않은 메시지 수
     * @param lastMessage 마지막 메시지
     */
    override fun sendUpdate(
        userId: String,
        roomId: String,
        unreadCount: Int,
        lastMessage: String?
    ) {
        // 활성 사용자가 아닌 경우 로깅 없이 조용히 무시
        if (!activeUserIds.contains(userId)) {
            return
        }

        emitters[userId]?.let { emitter ->
            try {
                // 데이터 구조를 명확하게 하여 JSON 형식으로 보냄
                val data = mapOf(
                    "roomId" to roomId,
                    "unreadCount" to unreadCount,
                    "lastMessage" to (lastMessage ?: "")
                )

                emitter.send(SseEmitter.event().data(data))
            } catch (e: Exception) {
                logger.error { "Failed to send update to user: $userId, error: ${e.message}" }
                emitters.remove(userId)
                activeUserIds.remove(userId) // 활성 사용자에서도 제거
            }
        } ?: run {
            // 활성 사용자인데 emitter가 없는 경우만 로깅 (실제 문제 상황)
            logger.warn { "Active user has no SSE emitter: $userId" }
            activeUserIds.remove(userId) // 캐시에서 제거
        }
    }

    /**
     * 사용자에게 채팅방 생성 이벤트를 전송합니다.
     *
     * @param event 이벤트
     */
    override fun sendChatRoomCreatedEvent(event: ChatRoomCreatedEvent) {
        // 활성 사용자가 아닌 경우 조용히 무시
        if (!activeUserIds.contains(event.userId)) {
            return
        }

        emitters[event.userId]?.let { emitter ->
            try {
                emitter.send(SseEmitter.event().name("chatRoomCreated").data(mapOf("roomId" to event.roomId)))
            } catch (e: Exception) {
                logger.error { "Failed to send chatRoomCreated event to user: ${event.userId}" }
                emitters.remove(event.userId)
                activeUserIds.remove(event.userId)
            }
        } ?: run {
            logger.warn { "Active user has no SSE emitter for chatRoomCreated event: ${event.userId}" }
            activeUserIds.remove(event.userId)
        }
    }

    /**
     * 사용자에게 친구 추가 이벤트를 전송합니다.
     *
     * @param event 이벤트
     */
    override fun sendFriendAddedEvent(event: FriendAddedEvent) {
        // 활성 사용자가 아닌 경우 조용히 무시
        if (!activeUserIds.contains(event.userId)) {
            return
        }

        emitters[event.userId]?.let { emitter ->
            try {
                emitter.send(SseEmitter.event().name("friendAdded").data(mapOf("friendId" to event.friendId)))
            } catch (e: Exception) {
                logger.error { "Failed to send friendAdded event to user: ${event.userId}" }
                emitters.remove(event.userId)
                activeUserIds.remove(event.userId)
            }
        } ?: run {
            logger.warn { "Active user has no SSE emitter for friendAdded event: ${event.userId}" }
            activeUserIds.remove(event.userId)
        }
    }

    /**
     * SSE emitter에 Heartbeat를 주기적으로 전송합니다.
     *
     * @param emitter SSE emitter
     * @param userId 사용자 ID
     */
    private fun sendSseHeartBeat(emitter: SseEmitter, userId: String) {
        // 스레드 이름을 직접 설정하는 간단한 방법
        val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
            val thread = Thread(runnable, "sse-heartbeat-$userId")
            thread.isDaemon = true
            thread
        }

        // 15초마다 Heartbeat 전송
        scheduler.scheduleAtFixedRate({
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"))
                // 디버그 레벨로 낮춤 (너무 자주 발생하는 로그)
                logger.debug { "Heartbeat sent to user: $userId" }
            } catch (e: Exception) {
                logger.warn { "Failed to send heartbeat to user: $userId, error: ${e.message}" }
                emitters.remove(userId)
                activeUserIds.remove(userId) // 활성 사용자에서도 제거
                scheduler.shutdown()
            }
        }, 15, 15, TimeUnit.SECONDS)

        // 메모리 누수 방지 (애플리케이션 종료 시 스케줄러도 종료되도록)
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                scheduler.shutdown()
            } catch (e: Exception) {
                logger.error { "Failed to shutdown heartbeat scheduler" }
            }
        })
    }

}